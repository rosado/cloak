;; simple build system in Clojure
;; Roland Sadowski [szabla gmail com] http://www.haltingproblem.net

;; Copyright (c) 2008 Roland Sadowski. All rights reserved.  The use and
;; distribution terms for this software are covered by the Common
;; Public License 1.0 (http://www.opensource.org/licenses/cpl1.0.php)
;; which can be found in the file CPL.TXT at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software. 

;; Here's how it works every (task ...) and (file ...) call adds a map
;; to the *tasks* map {:task-name {:actions ... :deps ... :desc ...}}.
;; Then a graph is created and sorted topologically (any cycles are
;; detected + exception is thrown). task-table provides a translation
;; between task names and indices of the graph.

(ns rosado.cloak.main
  (:use rosado.math.graph)
  (:require [rosado.io :as io]))

(defstruct task-struct :actions :deps :desc)

(def *tasks* (ref {}))					;holds tasks and actions
(def *queue*)							;holds sorted tasks
(def #^{:private true}
	 task-table (ref {:to-int {} :to-task {}}))
(def #^{:private true}
	 task-order)

(def *verbose* false)
(def *error-handler* println)

(defmulti to-task class)

(defmethod to-task java.lang.Integer [index]
  ((@task-table :to-task) index))

(defmethod to-task clojure.lang.Keyword [kw]
  ((@task-table :to-int) kw))

(defmethod to-task java.lang.String [fname]
  ((@task-table :to-int) fname))

(defn add-task [task-name task-info]
  (dosync (ref-set *tasks* (assoc @*tasks* task-name task-info))))

(defn- annotate-task 
  "Adds metadata to task. Does not save it in *tasks*."
  [tsk kw val]
  (let [t (to-task tsk)]
	(with-meta t (merge (meta t) {kw val}))))

(defn- task-annotations 
  "Returns annotations (meta-data) of a task."
  [tsk]
  (meta (to-task tsk)))

(defn do-task [task-name]
  (let [tsk (@*tasks* task-name)]
	(if-let [prefun (tsk :pre-check)]
		(if (prefun)
		  (when-let [actions (tsk :actions)]
			(actions))
		  (println " * skipping"))
	  (when-let [actions (tsk :actions)]
		  (actions)))))

(defn clear-tasks
  "Clears task table and *tasks* map (which holds defined tasks)"
  []
  (dosync
   (ref-set *tasks* {})
   (ref-set task-table {})))

(derive clojure.lang.LazilyPersistentVector ::Dependencies)
(derive clojure.lang.PersistentVector ::Dependencies)
(derive clojure.lang.IPersistentList ::Actions)

(defmulti parse-task (fn [mp elems] (class (first elems))))

(defmethod parse-task ::Dependencies [mp elems]
  (assoc mp :deps (first elems)))

(defmethod parse-task java.lang.String [mp elems]
  (assoc mp :desc (first elems)))

(defmethod parse-task ::Actions [mp elems]
  (assoc mp :actions `(fn [] (do ~@elems))))

(defmethod parse-task nil [mp elems] ;dummy task, no actions
  (assoc mp :actions `(fn[] nil))) 

(defn- to-task-struct [sequ]
  (loop [r sequ tsk (struct task-struct nil nil nil)]
	(if (not (tsk :actions))			;:actions should be added last
	  (recur (rest r) (parse-task tsk r))
	  tsk)))

;; throws exception if task already defined
(defn- fail-if-defined [task-name]
  (when (contains? @*tasks* task-name)
	(throw (Exception. "Task already defined."))))

(defmacro task [task-name & rst]
  (fail-if-defined task-name)
  (let [task (to-task-struct rst)]
	`(add-task ~task-name ~task)))

(defn- pre-check-fn [file-name fnames]
  `(fn [#^String e#]
	 (let [f# (java.io.File. ~file-name)
		   o# (java.io.File. e#)]
	   (if (not (io/exists? o#))
		 (let [msg# (format "File dependency not met: %s" e#)]
		   (println "Failure:" msg#)
		   (throw (Exception. msg#))))
	   (if (io/exists? f#)
		 (io/newer? o# f#)
		 true))))

(defmacro file [file-name & rst]
  (fail-if-defined file-name)
  (let [task (to-task-struct rst)
		fnames (doall (filter #(isa? (class %) String) (:deps task)))
		pre-check  `(fn [] (some ~(pre-check-fn file-name fnames) (list ~@fnames)))
		ftask (assoc task :pre-check pre-check)]
	`(add-task ~file-name ~ftask)))

(defn- make-table
  "Makes a dispatch table between task names and indices."
  [task-map]
  (let [ks (keys task-map) indices (range 1 (inc (count ks)))]
	{:to-int (zipmap ks indices) :to-task (zipmap indices ks)}))

(defn- task-names [] (-> @task-table :to-int keys))

(defn- task-indices [] (-> @task-table :to-int vals))

(defn init-tasks []
  (dosync (ref-set task-table (make-table @*tasks*))))

;; function used for resolving dependencies
;; 
(def
 #^{:private true}
 sort-tasks (make-dfs
			 (:tree-edge? (fn [g a b]
							(not (discovered? g b))))
			 (:back-edge? (fn [g a b]
							;(println "BACK-EDGE?")
							(not (tag? g b :post))))
			 (:back-edge-hook (fn [arg-m [a b]]
								(*error-handler* (format "Circular dependency: %s <=> %s."
														 (to-task a)
														 (to-task b)))
								(throw (Exception. "Dependency graph not is not a DAG"))))
			 (:increment-pre #(inc %1))
			 (:increment-post #(inc %1))
			 (:mark-pre-visited #(tag-vertex %1 %2 :pre %3))
			 (:mark-post-visited (fn [g vi cnt]
									 (when (= (tag? g vi :compo) 1) 
									   (set! *queue* (conj *queue* vi)))
									 (tag-vertex g vi :post cnt)))
			 (:increment-component (fn [cnt]
									 (inc cnt)))
			 (:mark-component (fn [g v cn]
							    ;(println "mark-component: " [g v cn])
								(tag-vertex g v :compo cn)))))

(defn- add-task-vertex [g index]
  (add-vertex g
			  index
			  (make-vertex {}
						   (let [alist (map #(to-task %1)
											(:deps (@*tasks* (to-task index))))]
							 (when alist alist)))))

(defn make-task-graph [tasks]
  (let [tab (init-tasks)
		g (make-graph (count tasks))]
	(reduce add-task-vertex g (task-indices))))

(defn execute-task [task-kw]
  (binding [*queue* []]
	(when *verbose*
	  (println "ZADANIA: " (task-indices) "//" (task-names)))
	(let [g (sort-tasks (make-task-graph @*tasks*) (to-task task-kw))])
	(doseq [q *queue*]
		(println "== Executing task" (to-task q))
	  (try
	   (do-task (to-task q))
	   (catch Exception e
		 (*error-handler* "Error executing task" q)
		 (throw e)))
	  (newline)
	  (println "Done."))))

