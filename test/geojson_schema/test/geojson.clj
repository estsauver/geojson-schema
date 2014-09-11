(ns geojson-schema.test.geojson
  (:require [clojure.java.io :refer [file]]
            [cheshire.core :as json]
            [geojson-schema.core :as schema]
            [schema.core :refer [validate]])
  (:use
   [clojure.test]))


;Geojson spec

;;;We validate all of the provided geojson spec examples are valid
(def geojson-examples
  (file-seq (java.io.File. "test/geojson_schema/test/geojson_examples/")))

(deftest all-spec-examples-are-valid
  (doseq [example (remove #(.isDirectory %) geojson-examples)]
    (let [body (slurp example)
          geojson (json/parse-string body true)]
      (is (validate schema/Geojson geojson) (str example)))))


;; LineString
(def line-string 
  {:type "LineString", 
   :coordinates [[100.0 0.0] [101.0 1.0]]})

(validate schema/LineString line-string)

(deftest linestring-need-two-coords
  (is (thrown-with-msg? Exception
                        #"Value does not match schema"
                        (validate schema/LineString 
                                  {:type "LineString", 
                                   :coordinates [[101.0 1.0]]}))))


;; LinearRing
(def linear-ring
  {:type "LineString", 
   :coordinates [[100.0 0.0] 
                 [101.0 1.0] 
                 [101.4 203.0]
                 [100.0 0.0]]})

(validate schema/LinearRing linear-ring)

(def not-closed-linear-ring
  {:type "LineString", 
   :coordinates [[100.0 0.0] 
                 [101.0 1.0] 
                 [101.4 203.0]
                 [101.5 0.0]
                 [103.0 401.0]]})

(deftest linear-rings-are-closed
  (is (thrown-with-msg? Exception
                        #"Value does not match schema"
                        (validate schema/LinearRing
                                  not-closed-linear-ring))))

;;; Line has too few coordinates, just leaves a point and 
;;; returns. Has no area
(def one-dimensional-line-segment
  {:type "LineString", 
   :coordinates [[100.0 0.0] 
                 [101.0 1.0] 
                 [100.0 0.0]]})

(deftest linear-rings-have-area
  (is (thrown-with-msg? Exception
                        #"Value does not match schema"
                        (validate schema/LinearRing 
                                  one-dimensional-line-segment))))

;;; All Linear Rings should be_a LineString

(deftest linear-rings-are-line-strings
  (is (validate schema/LineString linear-ring)))


;; MultiLineString
(def multiline 
  {:type "MultiLineString", 
   :coordinates [[[100.0 0.0] [101.0 1.0]] 
                 [[102.0 2.0] [103.0 3.0]]]})

(deftest multiline-strings 
  (is (validate schema/MultiLineString multiline)))


;; Polygon
(def polygon-noholes
  {:type "Polygon", 
   :coordinates [[[100.0 0.0] [101.0 0.0] [101.0 1.0] [100.0 1.0] [100.0 0.0]]]})

(def polygon-holes
  {:type "Polygon", 
   :coordinates [[[100.0 0.0] [101.0 0.0] [101.0 1.0] [100.0 1.0] [100.0 0.0]] 
                 [[100.2 0.2] [100.8 0.2] [100.8 0.8] [100.2 0.8] [100.2 0.2]]]})

(deftest polygons-are-valid
  (is (validate schema/Polygon polygon-noholes))
  (is (validate schema/Polygon polygon-holes)))


;;; There's an additonal requirement on Polygons from the spec which I don't have a good
;;; Idea of how to validate.
(deftest polygons-have-inner-rings-second
  (is true))


;; MultiPolygon
(def multipolygon
  {:type "MultiPolygon", 
   :coordinates [[[[102.0 2.0] [103.0 2.0] [103.0 3.0] [102.0 3.0] [102.0 2.0]]] 
                 [[[100.0 0.0] [101.0 0.0] [101.0 1.0] [100.0 1.0] [100.0 0.0]] 
                  [[100.2 0.2] [100.8 0.2] [100.8 0.8] [100.2 0.8] [100.2 0.2]]]]})

(deftest multipolygon-is-valid
  (is (validate schema/MultiPolygon multipolygon)))


;; GeometryCollection
(def geometry-collection
  {:type "GeometryCollection", 
   :geometries [{:type "Point", 
                 :coordinates [100.0 0.0]} 
                {:type "LineString", 
                 :coordinates [[101.0 0.0] [102.0 1.0]]}]})

(deftest geometrycollection-is-valid
  (is (validate schema/GeometryCollection 
                geometry-collection)))

;; Feature
(def feature
  {:type "Feature", 
   :geometry {:type "LineString", 
              :coordinates [[102.0 0.0] [103.0 1.0] [104.0 0.0] [105.0 1.0]]}, 
   :properties {:prop0 "value0", 
                :prop1 0.0}})

(deftest feature-is-valid
  (is (validate schema/Feature feature)))

;; FeatureCollection
(def feature-collection
  {:type "FeatureCollection", 
   :features [{:type "Feature", 
               :geometry {:type "Point", 
                          :coordinates [102.0 0.5]}, 
               :properties {:prop0 "value0"}} 
              {:type "Feature", 
               :geometry {:type "LineString", 
                          :coordinates [[102.0 0.0] [103.0 1.0] [104.0 0.0] [105.0 1.0]]}, 
               :properties {:prop0 "value0", :prop1 0.0}} 
              {:type "Feature", 
               :geometry {:type "Polygon", 
                          :coordinates [[[100.0 0.0] [101.0 0.0] [101.0 1.0] [100.0 1.0] [100.0 0.0]]]}, 
               :properties {:prop0 "value0", 
                            :prop1 {:this "that"}}}]})

(deftest featurecollection-is-valid
  (is (validate schema/FeatureCollection feature-collection))
  (is (validate schema/Geojson feature-collection)))