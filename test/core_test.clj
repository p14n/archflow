(ns core-test
  (:require [clojure.test :refer [deftest testing is]]
            [core :as c]))


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
    (is (= [[:function-doesnt-receive #'core-test/verify-account :monkey-dead]]

           (c/check-system-element {:event :monkey-dead
                                    :in [:commands]
                                    :out {:account-verified :new-accounts
                                          :account-failed :notifications}
                                    :targets {:any [#'verify-account]}})))))