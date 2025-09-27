(ns com.p14n.archflow.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.p14n.archflow.core :as c]
            [com.p14n.archflow.example :as e]))

(deftest check-helper-functions
  (testing "Get all channels from system"
    (is (= #{:commands :new-accounts :notifications}
           (c/all-channels e/system))))
  (testing "Get all handlers for a given channel"
    (is (= #{#'e/verify-account}
           (c/handlers-for-channel e/system :commands))))
  (testing "Get all handlers for a given event"
    (is (= #{#'e/send-email}
           (c/handlers-for-event e/system :account-failed))))
  (testing "Get output channel for event"
    (is (= :notifications
           (c/output-channel-for-event e/system :account-requested :account-failed)))))

(defn verify-account
  {:receives #{:account-requested}
   :returns #{:account-verified
              :account-failed}}
  [_ctx _event])


(deftest check-system
  (testing "Function doesn't produce expected events"
    (is (= [[:function-output-not-handled :account-requested #{:account-failed :account-verified}]
            [:expected-output-not-produced :account-requested #{:monkey-dead}]]

           (c/check-system-element {:event :account-requested
                                    :in [:commands]
                                    :out {:monkey-dead :notifications}
                                    :targets {:any [#'verify-account]}}))))

  (testing "Function doesn't receive expected events"
    (is (= [[:function-doesnt-receive #'verify-account :monkey-dead]]

           (c/check-system-element {:event :monkey-dead
                                    :in [:commands]
                                    :out {:account-verified :new-accounts
                                          :account-failed :notifications}
                                    :targets {:any [#'verify-account]}})))))