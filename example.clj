(ns clojee.core)
3 4 5 3.2 3/4 032234abc/242&4_&$["" abc
8675%#309 ; number, symbol
8675\u ; number, char
'[03'%a'b] #! uh-oh no no this is a comment! 8676
\too bad 3so "sad"
'[""123""abc""\\abc] \[abc true nil false #f ; not sure what #f is
; syntax
;   {let* [a v1 b v2 c v3] form}
#"oopsy \" \\ \u1234 \n \01234
" *e *1 *file* 123 \newline 
 "oopsy \" \\ \u1234 \n \01234
"
%1 %& #(%1 %&) %abc %123\abc
(fn* fn fn* fn def)
let*;
loop*\
letfn*"" nil
reify*@x
.^
do~
if`
clojure.core/import*,
[fn*#{}] *4 *1 *2 *3 *e &
fn  unquote unquote-splicing 
(and (type (+ 1))) (var)
fn;
\a a \ta \newline[] \@
(defn make-do
  [forms]
  {:type "do"
   :forms forms})

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

(def eg1
  [;(make-def 'id (make-fn '(x) 'x))
;   (make-def 'a 'True)
;   (make-def 'b 'False)
;   (make-def 'z (make-app 'a '[a b c]))
   (make-def 'f 'True)
   (make-app 'f '[x y])
   (make-let '[[f g]] (make-let '[[f h]] 'q))
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

(def root-env (new-env #{} {:depth 0}))
(def root-state {:bindings '#{True False}})

(defn prn-eg
  [& args]
   (let [[l s] (apply run-eg args)]
     (doseq [x l]
       (prn x))
     (prn s)))

