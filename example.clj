(ns clojee.core)
3 4 5 3.2 3/4 032234abc/2424["" abc
8675#309
\too bad 3so "sad"
'[""123""abc""\\abc]
; syntax
;   {let* [a v1 b v2 c v3] form}
;       warning: shadowing outer variable
;       error: duplicate names
;       error: in values of bindings, form
;   {def q form}
;       error: q already defined
;   q
;       error: q not resolvable
;   (f x y)
;   {do form1 form2 form3}
;   {fn* [a b c] form}
;       error: duplicate names
;   {set! x form}
;   {loop* [x v1 y v2] form_including_recur}
;       - no recur: warning, suggest let*
;       - nested loops: warning, recur may not work as expected
;       - duplicate var names: error
;   {if pred then else}
;     {if true  a b} -> a
;     {if false a b} -> b
#"oopsy \" \\ \u1234 \n \01234
"
 "oopsy \" \\ \u1234 \n \01234
"
(oops oops blar* stop def defn type a a* b b* def stop)
(fn* fn fn* fn def)
fn*;
fn*\
fn*"" nil
fn*@x
fn*^
fn*~
fn*`
fn*,
[fn*#{}]
fn
fn;
\a a \ta \newline[] \@
(defn make-do
  [forms]
  {:type "do"
   :forms forms})

(defn make-fn
  [symbols body]
  {:type "fn*"
   :params symbols
   :body body})

(defn make-set!
  [symbol value]
  {:type "set!"
   :symbol symbol
   :value value})

(defn make-if
  [pred then else]
  {:type "if"
   :pred pred
   :then then
   :else else})

(defn make-loop
  [bindings form]
  {:type "loop"
   :bindings bindings
   :form form})

(defn make-let
  [bindings body]
  {:type "let*"
   :bindings bindings
   :body body})

(defn make-def
  [symbol value]
  {:type "def"
   :symbol symbol
   :value value})

(defn make-app
  [f args]
  {:type "application"
   :function f
   :arguments args})


(def eg1
  [;(make-def 'id (make-fn '(x) 'x))
;   (make-def 'a 'True)
;   (make-def 'b 'False)
;   (make-def 'z (make-app 'a '[a b c]))
   (make-def 'f 'True)
   (make-app 'f '[x y])
   (make-let '[[f g]] (make-let '[[f h]] 'q))
;   (make-let '[[f g] [f h]] 'f)
;   (make-let '[[f g] [x z]] (make-app 'f '[x y]))])
;   (make-def '. (make-fn '(f g x) '(f (g x))))
;   (make-def 'd (make-fn '(x) '(x x)))
;   (make-def 't (make-fn '(f x) '(. f f x)))])
  ])


(defn my-resolve
  [sym env state]
  (cond 
    (nil? env)
      (if (contains? (state :bindings) sym)
          "root state")
    (contains? (env :bindings) sym) (env :depth)
    :else (recur sym (env :parent) state)))

(defn new-env
  [bindings old-env]
  (doseq [x bindings]
    (or (symbol? x)
        (throw (new Exception "new-env: requires symbols for bindings"))))
  {:bindings bindings
   :parent old-env
   :depth (+ 1 (:depth old-env))})


(defn f-symbol
  [node log env state]
  [(cons {:symbol node, 
          :type "symbol", 
          :resolution (my-resolve node env state)}
         log)
   state])

(defn is-deffed
  [symbol log bindings]
  (cons {:type "def-check" :symbol symbol
         :is-deffed (contains? bindings symbol)} log))

(defn f-def
  [node log env state]
  (let [log-2 (is-deffed (node :symbol) log (state :bindings))
        state-2 {:bindings (conj (state :bindings) (node :symbol))}] ; TODO is there a better way to copy most of a table, changing only 1 key?
    (f-node (node :value) log-2 env state-2)))
    
(defn m-seq
  ([nodes log env state] (m-seq f-node nodes log env state))
  ([f nodes log env state]
   (loop [log-n log node-n nodes state-n state]
     (if (empty? node-n)
         [log-n state-n]
         (let [[log-z state-z] (f (first node-n) log-n env state-n)]
           (recur log-z (rest node-n) state-z))))))

(defn shadowing?
  [sym log env state]
  (let [r (my-resolve sym env state)]
    (if (nil? r)
        [log state]
        [(cons {:type "shadowing" :symbol sym :location r} log)
         state])))

(defn f-let
  "recurs on: value of each binding, form"
  ; todo: unique symbols?
  [node log env state]
  (let [syms (map first (node :bindings))
        [log-1 state-1] (m-seq shadowing? syms log env state)]
    (let [[log-2 state-2] (m-seq (map second (node :bindings)) log-1 env state-1)]
      (let [sym-set (apply hash-set syms)
            log-3 (if (not (= (count syms) (count sym-set)))
                      (cons {:type "duplicate symbol in let", :symbols syms} log-2)
                      log-2)]
        (f-node (node :body)
                log-3
                (new-env sym-set env)
                state-2)))))

(defn f-app
  [node log env state]
  (let [[log-2 state-2] (f-node (node :function) log env state)]
    (m-seq (node :arguments) log-2 env state-2)))

(def actions
 {"symbol"      f-symbol
  "def"         f-def
  "application" f-app
  "let*"        f-let})

(defn my-type
  [node]
  (cond
    (symbol? node) "symbol"
    (map? node) (node :type)
    :else (throw (new Exception (str "unrecognized node -- " (if (nil? node) "nil" node))))))

(defn f-node
  [node log env state]
;  (do (prn (str "checking ..." node " in " env)))
  (let [type (my-type node)]
    (if (contains? actions type)
        ((actions type) node log env state)
        (throw (new Exception (str "unrecognized node type -- " node))))))


(def root-env (new-env #{} {:depth 0}))
(def root-state {:bindings '#{True False}})

(defn run-eg
  ([] (run-eg (second eg1)))
  ([node] (run-eg node '() root-env root-state))
  ([log env state]
    (m-seq eg1 log env state))
  ([node log env state]
    (f-node node log env state)))

(defn prn-eg
  [& args]
   (let [[l s] (apply run-eg args)]
     (doseq [x l]
       (prn x))
     (prn s)))

