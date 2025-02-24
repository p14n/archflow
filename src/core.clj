(ns core
  (:require [clojure.set :as s]))


(defn check-fn-receives [event f]
  (when (-> f meta :receives event not)
    [:function-doesnt-receive f event]))

(defn check-system-element [{:keys [event out targets]}]
  (let [all-functions (->> targets
                           vals
                           (apply concat))
        all-function-returned-events (->> all-functions
                                          (map meta)
                                          (map :returns)
                                          (apply s/union))
        all-element-returned-events (->> out keys set)
        not-handled (s/difference all-function-returned-events
                                  all-element-returned-events)
        not-produced (s/difference all-element-returned-events
                                   all-function-returned-events)]
    (concat
     (when (not-empty not-handled) [[:function-output-not-handled event not-handled]])
     (when (not-empty not-produced) [[:expected-output-not-produced event not-produced]])
     (->> all-functions
          (mapv (partial check-fn-receives event))
          (remove nil?)
          (vec)))))

(defn check-system [s]
  (->> s
       (map check-system-element)
       (apply concat)
       (remove empty?)
       (vec)))

