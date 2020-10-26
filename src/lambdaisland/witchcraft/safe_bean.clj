(ns lambdaisland.witchcraft.safe-bean
  (:refer-clojure :exclude [bean]))

(defn bean
  "Like clojure.core/bean but ignore exceptions"
  [^Object x]
  (cond
    (nil? x)
    x

    (map? x)
    x

    (vector? x)
    x

    :else
    (let [c (. x (getClass))
	  pmap (reduce (fn [m ^java.beans.PropertyDescriptor pd]
		         (let [name (. pd (getName))
			       method (. pd (getReadMethod))]
			   (if (and method (zero? (alength (. method (getParameterTypes)))))
                             (try
			       (assoc m (keyword name)
                                      (fn []
                                        (try
                                          (clojure.lang.Reflector/prepRet (.getPropertyType pd) (. method (invoke x nil)))
                                          (catch Exception e
                                            ::exception
                                            ))))
                               (catch Exception e
                                 m))
			     m)))
		       {}
		       (seq (.. java.beans.Introspector
			        (getBeanInfo c)
			        (getPropertyDescriptors))))
	  v (fn [k] ((pmap k)))
          snapshot (fn []
                     (reduce (fn [m e]
                               (assoc m (key e) ((val e))))
                             {} (seq pmap)))
          thisfn (fn thisfn [plseq]
                   (lazy-seq
                    (when-let [pseq (seq plseq)]
                      (cons (clojure.lang.MapEntry/create (first pseq) (v (first pseq)))
                            (thisfn (rest pseq))))))]
      (proxy [clojure.lang.APersistentMap]
          []
        (iterator [] (clojure.lang.SeqIterator. ^java.util.Iterator (thisfn (keys pmap))))
        (containsKey [k] (contains? pmap k))
        (entryAt [k] (when (contains? pmap k) (clojure.lang.MapEntry/create k (v k))))
        (valAt ([k] (when (contains? pmap k) (v k)))
	  ([k default] (if (contains? pmap k) (v k) default)))
        (cons [m] (conj (snapshot) m))
        (count [] (count pmap))
        (assoc [k v] (assoc (snapshot) k v))
        (without [k] (dissoc (snapshot) k))
        (seq [] (thisfn (keys pmap)))))))

(defmacro bean->
  "Like ->, but call bean on the initial value, and on each intermediate result"
  [x & forms]
  (loop [x x, forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (with-meta `(~(first form) (bean ~x) ~@(next form)) (meta form))
                       `(~form (bean ~x)))]
        (recur threaded (next forms)))
      `(bean ~x))))
