;; simple build system in Clojure
;; Roland Sadowski [szabla gmail com] http://www.haltingproblem.net

;; Copyright (c) 2008 Roland Sadowski. All rights reserved.  The use and
;; distribution terms for this software are covered by the Common
;; Public License 1.0 (http://www.opensource.org/licenses/cpl1.0.php)
;; which can be found in the file CPL.TXT at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software. 

(ns rosado.cloak
  (:use rosado.cloak.main))

(def *progname* "cloak")
(def *default-cloak-file* "CLOAK")
(def *default-task* :default)
(def *describe-only* false)
(def *target-queue* [])
(def path-sep #^java.lang.String (java.io.File/separator))

(defn error [& args]
  (apply println args))

(defn- load-tasks
  "Loads tasks from file (*default-cloak-file*) and creates
  task-table for use by other fns"
  []
  (clear-tasks)
  (try
   (load-file *default-cloak-file*)
   (catch Exception e
	 (error "Error loading cloak file.")
	 (error (.getMessage e))
	 (throw e))))

(defn run-tasks
  "Run given tasks. Aborts on first failed task."
  [kwords]
  (println "Running tasks:" (apply str (interpose " "(map str kwords))))
  (doseq [kw kwords]
	  (when-not (contains? @*tasks* kw)
		(error "No such task:" kw)
		(throw (Exception. "Specified task is not defined."))))
  (doseq [kw kwords]
	  (try
	   (binding [*error-handler* error]
		 (execute-task kw))
	   (catch Exception e
		 (error "Error: Couldn't finish task" kw)
		 (error (.getMessage e))
		 (throw e)))))

(defn print-desc
  "Prints task descriptions."
  [taskmap]
  (do
	(doseq [t (for [key (keys taskmap)]
				(assoc (@*tasks* key) :name key))]
		(print (t :name) "\t" )
	  (if (t :desc)
		(println (t :desc))
		(println)))))

(defn run-program []
  (when *default-cloak-file*
	(load-tasks))						   ;lets try to load the tasks from file
  (try
   (init-tasks)
   (catch Exception e
	 (error "Error initializing tasks")
	 (error (.getMessage e))
	 (throw e)))
  ;; perform actions
  (try
   (cond *describe-only* (print-desc @*tasks*)
		 (empty? *target-queue*) (run-tasks [*default-task*]) 
		 :else (run-tasks *target-queue*))))

(defn print-help []
  (println "RTFM!")) ;TODO

(defn print-usage []
  (error (format "usage: %s [options] [task-name]" *progname*))
  (error (format "try '%s -h' for more information." *progname*)))

(def *cmd-line-opts* {"-d" nil
					  "-f taskfile" nil
					  "-t" nil
					  "-h" nil})

;; parses command line arguments
(defmulti parse-arg first)

(defmethod parse-arg "-d" [args]
  (binding [*describe-only* true]
	(parse-arg (rest args))))

(defmethod parse-arg "-t" [args]
  (println "Following actions won't be performed: ")
  (when (some #{"-h" "-d"} (rest args))
	(print-usage)
	(throw (Exception. "Wrong parameters.")))
  (parse-arg (rest args)))

(defmethod parse-arg "-h" [args]
  (if (or (rest args) *describe-only*)
	(do
	  (print-usage)
	  (throw (Exception. "Wrong parameters.")))
	(print-help)))

(defmethod parse-arg "-f" [args]
  (when-not (second args)
	(print-usage)
	(throw (Exception. "Missing file argument (-f ...)")))
  (let [cfname (second args)
		rargs (rrest args)]
	(if (some #{"-h"} rargs) ;those args don't make sense now
	  (do
		(print-usage)
		(throw (Exception. "Wrong parameters.")))
	  (binding [*default-cloak-file* cfname]
		(parse-arg rargs)))))

(defmethod parse-arg :default [args]
  (let [tnames (vec (map keyword args))] ;remaining params are task names
	(binding [*target-queue* tnames]
	  (run-program))))

(defmethod parse-arg nil [args]
  (run-program))

(defn -main [& args]
  (try
   (parse-arg args)
   (catch Exception e
	 (println "Error:" (.getMessage e))
	 (System/exit 1))))

;; (binding [*warn-on-reflection* false]
;; ;  (parse-arg *command-line-args*)
;;   (main-fun))