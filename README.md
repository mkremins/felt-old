# felt

A prototype of the kind of simulated storyworld we might use for the [cozy mystery project](https://github.com/ExpressiveIntelligence/CozyMysteryCo). Characters have personalities based on tropes and hold certain values, which they use to judge the actions of other characters. Actions have preconditions and must be somehow *motivated*.

## Usage

This section assumes you've got [Leiningen](https://leiningen.org/) already installed.

Start up a Clojure REPL:

```
lein repl
```

Load in the `felt.db` namespace:

```clojure
(require 'felt.db)

(in-ns 'felt.db)
```

Create a new world:

```clojure
(gen-world!)

; This will also list all the characters that were generated
; and the initial relationships between them.
```

Sample some actions the characters might perform:

```clojure
(draw-actions @conn)

(draw-action @conn 10)

(draw-actions-for-char @conn "Chris")

(draw-actions-for-char @conn "Chris" 3)
```

Sample actions, then pick one to perform:

```clojure
(draw-actions @conn)

(let [action (nth *1 0)]
  (perform-action! conn action))

; *1 = the value of the previously entered expression,
; which here will be a vector of several possible actions.
```

Seek out narratively interesting situations:

```clojure
(find-mutual-likes @conn)
; A and B like one another

(find-mutual-dislikes @conn)
; A and B dislike one another

(find-mismatches @conn)
; A likes B, but B dislikes A

(find-secret-crushes @conn)
; A has a crush on B

(find-jealousies @conn)
; A is jealous of B, because A has a crush on C, and C likes B

(find-common-values @conn)
; lists all pairs of characters who hold a common value

(find-chars-by-value @conn :communalism)
; lists all characters who hold :communalism as a value
```

Blow away the current world and create a new one:

```clojure
(gen-world!)
```
