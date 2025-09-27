(ns com.p14n.flowarch.d2
  (:require [clojure.set :as set]))

(defn v->name
  "Helper to convert #'function-name to function-name tring"
  [v]
  (some-> v symbol name))

(defn get-function-deployment
  "Helper to get deployment name for a function"
  [fn-name deployments]
  (first (for [[dep fns] deployments
               :when (contains? fns fn-name)]
           dep)))

(defn ->deployment-names-map
  [vars]
  (->> vars
       (map (fn [d]
              [(v->name d) (->> d
                                (deref)
                                (map v->name)
                                (set))]))
       (into {})))

(defn system->d2
  [system deployment-vars]
  (let [deployments (->deployment-names-map deployment-vars)

        ;; Build connections from system definition
        connections (->> (for [route system
                               in-queue (:in route)
                               tfn (get-in route [:targets :any])
                               :let [from-deployment (get-function-deployment
                                                      (v->name tfn)
                                                      deployments)]]
                           (if (:out route)
                             (vec (for [[out-event out-queue] (:out route)]
                                    {:target {:deployment from-deployment
                                              :fn tfn
                                              :events (-> tfn meta :receives)}
                                     :out-event out-event
                                     :out-queue out-queue
                                     :in-queue in-queue}))
                             [{:target {:deployment from-deployment
                                        :fn tfn
                                        :events (-> tfn meta :receives)}
                               :in-queue in-queue}]))
                         (apply concat)
                         (vec))
        targets-by-queue-event
        (reduce (fn [acc {:keys [in-queue target]}]
                  (reduce (fn [acci in-event]
                            (update-in acci [in-queue in-event]
                                       #(concat % [(-> target :fn)])))
                          acc
                          (-> target :events)))
                {}
                connections)
        get-q-types #(->> connections (map %) (set))
        orphans (->> (set/difference (get-q-types :in-queue)
                                     (get-q-types :out-queue))
                     (select-keys targets-by-queue-event))]

    (str
     ;; Define deployments
     (apply str
            (for [[dep fns] deployments]
              (format "%s: {\n  %s\n}\n"
                      dep
                      (apply str (interpose "\n  " fns)))))

     ;; Add connections
     "\n"
     (reduce (fn [acc conn]
               (let [{:keys [target out-event out-queue]} conn]
                 (->> (for [fn (get-in targets-by-queue-event [out-queue out-event])]
                        (format "%s.%s -> %s.%s: %s\n"
                                (:deployment target) (v->name (:fn target))
                                (-> fn (v->name) (get-function-deployment deployments)) (v->name fn) out-event))
                      (apply str)
                      (str acc))))
             "" connections)

     ;; Add command source
     (->> (map (fn [[q v]]
                 (->> v
                      (map (fn [[event funcs]]
                             (str (name q) "\n\n"
                                  (->> (for [f (set funcs)]
                                         (str (name q) " -> " (get-function-deployment (v->name f) deployments) "." (v->name f) ": " (name event) "\n"))
                                       (apply str)))))
                      (apply str))) orphans)
          (apply str)))))