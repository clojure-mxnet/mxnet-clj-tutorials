(ns mxnet-clj-tutorials.image-manipulation
  (:require
    [clojure.java.io :as io]

    [org.apache.clojure-mxnet.image :as mx-img]
    [org.apache.clojure-mxnet.ndarray :as ndarray]
    [org.apache.clojure-mxnet.shape :as mx-shape]

    [opencv4.core :as cv]
    [opencv4.utils :as cvu])

  (:import org.opencv.core.Mat java.awt.image.DataBufferByte))

(defn download!
  "Download `uri` and store it in `filename` on disk"
  [uri filename]
  (with-open [in (io/input-stream uri)
              out (io/output-stream filename)]
    (io/copy in out)))

(defn preview!
  "Preview image from `filename` and display it on the screen in a new window

  >>> (preview! \"images/cat.jpg\")
  >>> (preview! \"images/cat.jpg\" :h 300 :w 200)
  "
  ([filename]
   (preview! filename {:h 400 :w 400}))
  ([filename {:keys [h w]}]
   (-> filename
       cv/imread
       (cv/resize! (cv/new-size h w))
       cvu/imshow)))

(defn preprocess-mat
  "Preprocessing steps on a `mat` from OpenCV.
  Example of commons preprocessing tasks"
  [mat]
  (-> mat
      ;; Substract mean
      (cv/add! (cv/new-scalar 103.939 116.779 123.68))
      ;; Resize
      (cv/resize! (cv/new-size 400 400))
      ;; Maps pixel values from [-125, 125] to [0, 250]
      (cv/convert-to! cv/CV_8SC3 0.5)))
      ;; TODO: add cropping?


(defn mat->ndarray
  "Converts a `mat` from OpenCV to an MXNET `ndarray`"
  [mat]
  (let [h (.height mat)
        w (.width mat)
        c (.channels mat)]
    (-> mat
        cvu/mat->flat-rgb-array
        (ndarray/array [c h w]))))

(defn ndarray->mat
  "Convert a `ndarray` to an OpenCV `mat`"
  [ndarray]
  (let [shape (mx-shape/->vec ndarray)
        [h w _ _] (mx-shape/->vec (ndarray/shape ndarray))
        bytes (byte-array shape)
        mat (cv/new-mat h w cv/CV_8UC3)]
    (.put mat 0 0 bytes)
    mat))

(defn filename->ndarray!
  "Convert an image stored on disk `filename` into an `ndarray`

  `filename`: string representing the image on disk
  `shape-vec`: is the actual shape of the returned `ndarray`
  "
  [filename shape-vec]
  (-> filename
      cv/imread
      mat->ndarray))

(comment

  ;; Download a cat image from a `uri` and save it into `images/cat.jpg`
  (download! "https://raw.githubusercontent.com/dmlc/web-data/master/mxnet/doc/tutorials/python/predict_image/cat.jpg" "images/cat.jpg")

  ;; Preview an image from disk
  (preview! "images/cat.jpg")

  ;; Preview with different size
  (preview! "images/cat.jpg" {:h 300 :w 200})

  ;; Visualize preprocessing steps
  (-> "images/cat.jpg"
      cv/imread
      preprocess-mat
      cvu/imshow))

(comment
  (-> "images/dog.jpg"
      ;;
      (mx-img/read-image #_{:to-rgb true}) ;; optiona to-rgb, true by default
      ;; Resizing image to height = 400, width = 400
      (mx-img/resize-image 400 400)
      mx-img/to-image
      ;; Saving image to disk
      (javax.imageio.ImageIO/write "jpg" (java.io.File. "test2.jpg")))

  (-> "images/mnist_digit_8.jpg"
      (mx-img/read-image {:to-rgb true})
      ; mx-img/to-image
      ; (javax.imageio.ImageIO/write "jpg" (java.io.File. "test.jpg"))
      ; cvu/buffered-image-to-mat
      ;; Save Image To File
      ndarray->mat
      (cv/imwrite "test-digit.jpg"))
      ; cvu/imshow


  ;; Showing an image using `cvu/buffered-image-to-mat`
  (-> "images/dog.jpg"
      (mx-img/read-image {:to-rgb true})
      mx-img/to-image
      cvu/buffered-image-to-mat
      cvu/imshow)

  ;; Much Faster using custom `ndarray->mat`
  (-> "images/dog.jpg"
      (mx-img/read-image {:to-rgb false})
      ndarray->mat
      cvu/imshow))
