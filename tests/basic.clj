(ns rosado.cloak.test.basic
  (:use clojure.contrib.test-is)
  (:require [rosado.cloak :as cloak]))


;; testing command line parsing

(defn opt-f []
  (cloak/parse-arg ["-f"]))

(defn opt-t-h []
  (cloak/parse-arg ["-t" "-h"]))

(defn opt-h-t []
  (cloak/parse-arg ["-h" "-t"]))

(defn opt-h-word []
  (cloak/parse-arg ["-h" "some-word"]))

(deftest cmdline-opts
  (throws Exception (opt-f))
  (throws Exception (opt-t-h))
  (throws Exception (opt-h-t))
  (throws Exception (opt-h-word)))

(deftest simple-task
  (is (= nil (cloak/parse-arg ["a"])))
  (throws Exception (cloak/parse-arg ["nonexisiting-task"])))

(deftest circular-dep-task
  (throws Exception (cloak/parse-arg ["-f" "CIRCULAR" "a"]))
  (throws Exception (cloak/parse-arg ["-f" "CIRCULAR" "b"])))

(cloak/parse-arg ["-f" "ONCE" "a"])

(deftest run-only-once
  (is (= 1 @cloak.tests.once/*counter-a*)))

(cloak/parse-arg ["-f" "ONCE" "b" "b"])

(deftest run-only-once
  (is (= 1 @cloak.tests.once/*counter-b*)))

(run-tests)
