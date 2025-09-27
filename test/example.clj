(ns example
  (:require [d2 :as d2]
            [core :as c]))

(defn verify-account
  {:receives #{:account-requested}
   :returns #{:account-verified
              :account-failed}}
  [ctx event])

(defn open-account
  {:receives #{:account-verified}
   :returns #{:account-opened}}
  [ctx event])

(defn send-email
  {:receives #{:account-failed :account-opened}}
  [ctx event])

(def system
  [{:event :account-requested
    :in [:commands]
    :out {:account-verified :new-accounts
          :account-failed :notifications}
    :targets {:any [#'verify-account]}}

   {:event :account-verified
    :in [:new-accounts]
    :out {:account-opened :new-accounts}
    :targets {:any [#'open-account]}}

   {:event :account-failed
    :in [:notifications]
    :targets {:any [#'send-email]}}

   {:event :account-opened
    :in [:new-accounts]
    :targets {:any [#'send-email]}}])


(def accounts-deployment
  [#'verify-account
   #'open-account])

(def notifier-deployment
  [#'send-email])

(c/check-system system)

(spit "gen.d2" (d2/system->d2 system [#'accounts-deployment #'notifier-deployment]))




