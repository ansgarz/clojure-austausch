(ns app.core
  (:require
    [clojure.spec.alpha :as s]))

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
;; or we can conform a schema and value and let the value be returned upon success
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
  (println (type config))
  2)


;;cat takes pairs of key + predicate
(s/fdef domain-to-infra
        :args (s/cat :config string?)
        :ret int?)


;;(s/conform domain-to-infra (domain-to-infra "bla"))
(stest/instrument `domain-to-infra)
(domain-to-infra "string")


; ***** Generating data for test etc. *****
;; Define sample functions
(defn adder [x y] (+ x y))
(defn adder-faulty [x y] (+ (+ x y) 1))

;; Traditional Testing
(use 'clojure.test)
(deftest adder-test
  (is (= 4 (adder 2 2)))
  (is (= 7 (adder 3 4))))
(deftest adder-faulty-test
  (is (= 4 (adder-faulty 2 2)))
  (is (= 7 (adder-faulty 3 4))))

(run-tests 'app.core)

;; Generating data
;; Import libraries and
(require '[clojure.spec.gen.alpha :as gen])

;; generate a value for data types (spec/gen returns a generator)
(gen/generate (s/gen int?))
(gen/generate (s/gen nat-int?))
(gen/generate (s/gen string?))

;; generate a value for data structures (e.g. list/map resp. a vector)
(gen/generate (s/gen (s/cat :a int? :b string?)))
(gen/generate (s/gen (s/tuple int? string?)))

;; generate complexer data structure
(s/def ::street string?)
(s/def ::street-nbr (s/int-in 0 9999))
(s/def ::address (s/keys :req [::street ::street-nbr]))
(gen/generate (s/gen ::address))

;; generate a list of data for a data type
(gen/sample (s/gen int?))

;; generate values from a set
(gen/sample (s/gen #{:a :b 10 11 "ten" "eleven"}))

;; Testing
;; test or call a function with generated values
(adder (gen/generate (s/gen int?)) (gen/generate (s/gen int?)))
(adder-faulty (gen/generate (s/gen int?)) (gen/generate (s/gen int?)))

;; generate input values for a spec'd function and call the function
(s/fdef adder
  :args (s/cat :x int? :y int?)
  :ret int?
  :fn #(= (-> :ret %) (+ (-> % :args :x) (-> % :args :y))))
(s/exercise-fn `adder)

(s/fdef adder-faulty
  :args (s/cat :x int? :y int?)
  :ret int?
  :fn #(= (-> :ret %) (+ (-> % :args :x) (-> % :args :y))))
(s/exercise-fn `adder-faulty)

; Instrumenting and Testing
(require '[clojure.spec.test.alpha :as stest])

;; without using the function spec
(adder 0.5 0.5)

;; using the function spec
(stest/instrument `adder)
(adder 0.5 0.5)   ;resulting in error due to not using int parameters

;; check a function: generate arguments based on the :args spec for a function,
;;invoke the function, and check that the :ret and :fn specs were satisfied.
(stest/abbrev-result (first (stest/check `adder)) )
(stest/abbrev-result (first (stest/check `adder-faulty)) )
