;; simple automation system in Clojure
;; Roland Sadowski [szabla gmail com] http://www.haltingproblem.net/

;; this file is only for command line usage.
;; Assuming you have a script which lets you run clojure scripts:
;; $ clojure cloak_cmd.clj -- args

(ns rosado.cloak.cmd
  (:use [rosado.cloak :as cloak]))

(apply cloak/-main *command-line-args*)