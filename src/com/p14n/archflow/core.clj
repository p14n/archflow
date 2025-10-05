(ns com.p14n.archflow.core
  (:require [clojure.set :as s]))


(defn check-fn-receives [meta-fn event f]
  (when (-> f meta-fn :receives event not)
    [:function-doesnt-receive f event]))

(defn check-system-element [meta-fn {:keys [event out targets]}]
  (let [all-functions (->> targets
                           vals
                           (apply concat))
        all-function-returned-events (->> all-functions
                                          (map meta-fn)
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
          (mapv (partial check-fn-receives meta-fn event))
          (remove nil?)
          (vec)))))

(defn check-system
  ([s] (check-system s meta))
  ([s meta-fn]
   (->> s
        (map (partial check-system-element meta-fn))
        (apply concat)
        (remove empty?)
        (vec))))

(defn all-channels
  "Returns a set of all channels in the system"
  [system]
  (->> system
       (map (juxt :in :out))
       (map (fn [[event out]]
              (-> out vals (concat event))))
       (apply concat)
       (into #{})))

(defn x>> [s x]
  (println "x>>" s x)
  x)

(defn handlers-in-system
  "Returns a set of all handlers in the system"
  [system]
  (->> system
       (map :targets)
       (mapcat vals)
       (apply concat)
       (into #{})))

(defn handlers-for-channel
  "Returns a set of all handlers for a given channel"
  [system channel]
  (->> system
       (filter (fn [{:keys [in]}]
                 (some #{channel} in)))
       (handlers-in-system)))

(defn handlers-for-event
  "Returns a set of all handlers for a given event"
  [system ev]
  (->> system
       (filter (fn [{:keys [event]}]
                 (= ev event)))
       (handlers-in-system)))

(defn output-channel-for-event
  "Returns the output channel for a given event"
  [system incoming-event outgoing-event]
  (->> system
       (filter (fn [{:keys [event]}]
                 (= incoming-event event)))
       (map :out)
       (apply (partial merge-with concat))
       outgoing-event))
