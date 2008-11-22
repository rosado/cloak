(ns rosado.cloak.cmd
  (:use [rosado.cloak :as cloak]))

(binding [*command-line-args* *command-line-args*]
  (cloak/main-fun))