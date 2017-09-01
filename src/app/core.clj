(ns app.core
  (:require [clojure.spec.alpha :as s]))

;; define map with spec
(s/def ::my-config
  (s/keys :req [::url ::port ::server]
          :opt [::document-root]))

(def valid-config
  {::url "my-url" ::port "80" ::server "apache2"})

(def valid-config2
  {::url "another-url" ::port "80" ::server "apache2"})

(def invalid-config
  {::port "80" ::server "apache2" ::document-root "/root"})

(s/def ::infra-config
  (s/keys :req [::url ::port ::server ::liferay]
          :opt [::document-root]))

;; we can validate configs to return a boolean like so
(s/valid? ::my-config valid-config)
(s/valid? ::my-config invalid-config)
;; or we can conform a schema and value and let the value be returned upon successs
(s/conform ::my-config valid-config)
(s/conform ::my-config invalid-config)
;; and we can describe a schema
(s/describe ::my-config)

;;we have a function to explain why a value did not match a schema
(s/explain ::my-config invalid-config)
;;we can receive this error message as data too
(s/explain-data ::my-config invalid-config)

;; coll of can be passed a few more arguments
;; :kind - a predicate or spec that the incoming collection must satisfy, such as vector?
;; :count - specifies exact expected count
;; :min-count, :max-count - checks that collection has (<= min-count count max-count)
;; :distinct - checks that all elements are distinct
;; :into - one of [], (), {}, or #{} for output conformed value. If :into is not specified, the input collection type will be used.

;; Difference every & coll-of should be explained
(def config-collection
  (s/coll-of ::my-config :kind vector? :into ()))

(s/conform config-collection [valid-config valid-config2])

;; an example where we can bin keyword <-> value and display that relation with explain
(s/def ::ingredient (s/cat :quantity number? :unit keyword?))
(s/conform ::ingredient [2 :teaspoon])


;; Differences to schema
;; Spec does validation on functions a little bit different
(defn domain-to-infra
  "docstring"
  [config]
  (assoc config ::liferay "true"))

(s/fdef domain-to-infra
        :args (s/valid? ::my-config :config)
        :ret (s/valid? ::my-config ::infra-config))

(domain-to-infra invalid-config)