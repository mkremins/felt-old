(ns felt.db
  (:require [datascript.core :as d]))

;;; DB setup

(def schema
  {:trope  {:db/cardinality :db.cardinality/many}
   :trait  {:db/cardinality :db.cardinality/many}
   :values {:db/cardinality :db.cardinality/many}
   ; represents a relationship between two characters
   :ship   {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
   :src    {:db/valueType :db.type/ref}
   :dst    {:db/valueType :db.type/ref}
   ; action stuff
   :perp   {:db/valueType :db.type/ref} ; whodunnit?
   :harms  {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
   :helps  {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
   :harms-value {:db/cardinality :db.cardinality/many}
   :helps-value {:db/cardinality :db.cardinality/many}})

(def rules
  '[[(likes ?c1 ?c2)
     [?liking :charge :like] [?liking :src ?c1] [?liking :dst ?c2]]

    [(dislikes ?c1 ?c2)
     [?disliking :charge :dislike] [?disliking :src ?c1] [?disliking :dst ?c2]]

    [(neutral ?c1 ?c2)
     [?neutrality :charge :neutral] [?neutrality :src ?c1] [?neutrality :dst ?c2]]

    [(crush ?c1 ?c2)
     [?crush :secret-crush? true] [?crush :src ?c1] [?crush :dst ?c2]]

    [(jealous-of ?envier ?envied ?crushee)
     [?crush :secret-crush? true] [?crush :src ?envier] [?crush :dst ?crushee]
     [?liking :charge :like] [?liking :src ?crushee] [?liking :dst ?envied]
     [(not= ?envied ?envier)] ; you probably shouldn't be jealous of yourself
     ]

    [(harmed-by ?c1 ?c2)
     [?harm :harms ?c1] [?harm :perp ?c2]]

    [(helped-by ?c1 ?c2)
     [?help :helps ?c1] [?help :perp ?c2]]

    [(holds-value-harmed-by ?c1 ?c2)
     [?c1 :values ?v] [?harm :harms-value ?v] [?harm :perp ?c2]]

    [(holds-value-helped-by ?c1 ?c2)
     [?c1 :values ?v] [?help :helps-value ?v] [?help :perp ?c2]]

    [(motive-to-harm ?c1 ?c2)
     (or (dislikes ?c1 ?c2)
         (jealous-of ?c1 ?c2 _)
         (harmed-by ?c1 ?c2)
         (holds-value-harmed-by ?c1 ?c2))]

    [(motive-to-help ?c1 ?c2)
     (or (likes ?c1 ?c2)
         (helped-by ?c1 ?c2)
         (holds-value-helped-by ?c1 ?c2)
         (and [?c1 :values :communalism]
              (neutral ?c1 ?c2)))]

    [(scientist ?c)
     (or [?c :role :professor]
         [?c :role :student]
         [?c :role :astronomer]
         [?c :role :archivist])]

    ;; TODO not sure if this explicit approach is really needed,
    ;; but (not (scientist ?c)) was complaining about unbound vars
    [(nonscientist ?c)
     (or [?c :role :observatory-caretaker]
         [?c :role :groundskeeper]
         [?c :role :psychic]
         [?c :role :reporter]
         [?c :role :skier]
         [?c :role :volunteer]
         [?c :role :high-schooler]
         [?c :role :giftshopkeep]
         [?c :role :security]
         [?c :role :tourist]
         [?c :role :tour-guide])]

    [(directly-involves ?action ?char)
     (or [?action :perp ?char]
         [?action :harms ?char]
         [?action :helps ?char])

    [(directly-involves-both ?action ?c1 ?c2)
     (directly-involves ?action ?c1)
     (directly-involves ?action ?c2)]]
])

(defonce conn
  (d/create-conn schema))

;;; character generation

(def char-names [
"Ada" "Adrian" "Alexis" "Alice" "Amber" "Andy" "Audrey"
"Becca" "Bob" "Brian"
"Catherine" "Chris" "Cindy" "Colin"
"Dan" "Dave" "Dorothy"
"Edgar" "Eliza" "Emily" "Esteban" "Evan" "Evie"
"Fred"
"Georgia" "Grace"
"Heather" "Henry"
"Isaac" "Isabel" "Ivan"
"Jason" "Jess" "Jocelyn" "John" "Josie" "Julie"
"Karin" "Kate" "Kevin"
"Laura" "Lea" "Lex" "Louis"
"Mark" "Mary" "Maureen"
"Nathan" "Nick"
"Omar"
"Peter"
"Quinn"
"Rachel" "Ross"
"Sarah"
"Tracy"
"Vi"
"Will"
"Zed"
])

(def all-values
  ;; from https://docs.google.com/document/d/1jft74HzoOutj224O1Pfj-rSm9btxNjPqWo2o5WUtNRU/edit
  [:science :survival :communalism :funding :comfort :order :faith :progress])

(def primary-tropes
  ["Absent-Minded Professor" "Big Fun" "Boss" "Bad-to-the-bone"
   "Parent Figure" "Innocent" "Clown" "Nerd" "Tortured Artist"
   "Gentle Giant" "Scruffy and Gruff" "Regal Presence" "Seductive"
   "Dumb Muscle" "Elderly Master" "Know-it-all" "Strict/By-the-books"
   "Rugged" "Lone Wolf" "Athlete" "Eccentric"])

(def secondary-tropes
  ["Peacemaker" "Pessimist" "Optimist" "Introvert" "Extrovert" "Jerk"
   "Resigned" "Loyalist" "Friend Next Door" "Child" "Outspoken"
   "Mousey/Shy" "Conscience" "Imposter/Pretender" "Side Kick"
   "Astrology Buff" "Socially Awkward/Misses Cues"])

(def all-traits
  ;; from https://github.com/ExpressiveIntelligence/CozyMysteryCo/blob/master/asp/characterGen/identities.lp
  ;; note some duplicates to make them able to show up more than once in a single cast
  ["rich" "in debt" "dying" "important online" "celebrity" "outsider"
   "secret expert" "in hiding" "has funding" "has theory" "has theory"
   "secretly right" "protective of observatory" "protective of observatory"
   "hates this place"])

(def all-roles
  ;; from https://github.com/ExpressiveIntelligence/CozyMysteryCo/blob/master/asp/characterGen/identities.lp
  ;; note some duplicates to make them able to show up more than once in a single cast
  [:professor :professor
   :student :student :student :student
   :observatory-caretaker :groundskeeper :psychic :reporter :skier
   :astronomer :volunteer :high-schooler :giftshopkeep :security
   :tourist :tour-guide :archivist])

(defn gen-ship [id1 id2]
  (let [charge (rand-nth [:like :dislike :neutral])]
    {:type :ship
     :charge charge
     :secret-crush? (and (= charge :like) (< (rand) 0.2))
     :src id1
     :dst id2}))

;; TODO also generate professional & romantic relationships
(defn gen-cast []
  (let [size     7
        ids      (take size (map (comp - inc) (range)))
        names    (take size (shuffle char-names))
        tropes-a (take size (shuffle primary-tropes))
        tropes-b (take size (shuffle secondary-tropes))
        traits   (take size (shuffle all-traits))
        roles    (take size (shuffle all-roles))
        chars    (mapv #(-> {:type :char
                             :db/id %1
                             :name %2
                             :role %3
                             :trope [%4 %5]
                             :trait [%6]
                             :values (vec (take 2 (shuffle all-values)))})
                       ids names roles tropes-a tropes-b traits)
        pairs    (for [id1 ids id2 ids :when (not= id1 id2)] [id1 id2])
        charges  (map (fn [[id1 id2]] (gen-ship id1 id2)) pairs)]
    (into chars charges)))

;(defn find-professors [db]
;  (d/q '[:find :))

;(defn gen-romantic-ship [db]
;  ())

;;; DB helpers

(defn ->id [db thing]
  (cond
    (integer? thing)
      thing
    (string? thing)
      (d/q '[:find ?c . :in $ ?name :where [?c :name ?name]] db thing)
    (map? thing)
      (:db/id thing)
    :else
      (throw (str "can't convert to id: " (pr-str thing)))))

(defn id->name [db id]
  (:name (d/pull db [:name] id)))

(defn name->id [db name]
  (d/q '[:find ?c . :in $ ?name :where [?c :name ?name]] db name))

(defn entity [db id]
  ;; TODO this doesn't actually work. i cry
  (into {:db/id id} (d/touch (d/entity db id))))

(defn describe-char [db id]
  (let [id       (->id db id)
        ent      (d/pull db [:db/id :name :role :trope :trait :values] id)
        ship-ids (map first (d/q '[:find ?s :in $ ?c :where [?s :src ?c]] db id))
        ships    (map #(d/pull db [:charge :dst] %) ship-ids)
        ships    (group-by :charge ships)]
    (assoc ent
      :likes    (mapv #(id->name db (:db/id (:dst %))) (:like ships))
      :dislikes (mapv #(id->name db (:db/id (:dst %))) (:dislike ships))
      :neutral  (mapv #(id->name db (:db/id (:dst %))) (:neutral ships)))))

(defn all-char-ids [db]
  (map first (d/q '[:find ?c :where [?c :type :char]] db)))

;;; query functions – use these to mine for narratively interesting situations

(defn find-mismatches [db]
  (d/q '[:find ?n1 ?n2 :in $ %
         :where (likes ?c1 ?c2) (dislikes ?c2 ?c1)
                [?c1 :name ?n1] [?c2 :name ?n2]]
       db rules))

(defn find-mutual-likes [db]
  (d/q '[:find ?n1 ?n2 :in $ %
         :where (likes ?c1 ?c2) (likes ?c2 ?c1)
                [?c1 :name ?n1] [?c2 :name ?n2]]
       db rules))

(defn find-mutual-dislikes [db]
  (d/q '[:find ?n1 ?n2 :in $ %
         :where (dislikes ?c1 ?c2) (dislikes ?c2 ?c1)
                [?c1 :name ?n1] [?c2 :name ?n2]]
       db rules))

(defn find-secret-crushes [db]
  (d/q '[:find ?n1 ?n2
         :where [?crush :secret-crush? true] [?crush :src ?c1] [?crush :dst ?c2]
                [?c1 :name ?n1] [?c2 :name ?n2]]
       db))

(defn find-jealousies [db]
  "1 is jealous of 2, because 1 has a crush on 3, and 3 likes 2"
  (d/q '[:find ?n1 ?n2 ?n3 :in $ %
         :where (jealous-of ?c1 ?c2 ?c3)
                [?c1 :name ?n1] [?c2 :name ?n2] [?c3 :name ?n3]]
       db rules))

(defn find-chars-by-value [db value]
  (d/q '[:find ?n :in $ ?v :where [?c :values ?v] [?c :name ?n]] db value))

(defn find-common-values [db]
  (d/q '[:find ?n1 ?n2 ?v
         :where [?c1 :values ?v] [?c2 :values ?v]
                [(not= ?c1 ?c2)]
                [?c1 :name ?n1] [?c2 :name ?n2]]
       db))

;; TODO doesn't work
(defn find-history [db id]
  (d/q '[:find ?a :in $ % ?c
         :where (directly-involves ?a ?c)]
        db rules (->id db id)))

;; TODO doesn't work
(defn find-common-history [db id1 id2]
  (d/q '[:find ?a :in $ % ?c1 ?c2
         :where (directly-involves-both ?a ?c1 ?c2)]
       db rules (->id db id1) (->id db id2)))

;;; action generation

(def all-actions
  [;; generic social moves

   {:type :action
    :name :insult
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (motive-to-harm ?c1 ?c2)
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :significance :lowest}

   {:type :action
    :name :accuse-of-violating-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where (motive-to-harm ?c1 ?c2)
                    [?harm :harms-value ?v] [?harm :perp ?c2]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :helps-value [4] ; helps the value being "defended"?
    :significance :lowest}

   {:type :action
    :name :accuse-of-hypocrisy
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where (motive-to-harm ?c1 ?c2)
                    [?harm :harms-value ?v] [?harm :perp ?c2] [?c2 :values ?v]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :helps-value [4] ; helps the value being "defended"?
    :significance :low}

   {:type :action
    :name :ignore
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (motive-to-harm ?c1 ?c2)
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :significance :lowest}

   {:type :action
    :name :compliment
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (motive-to-help ?c1 ?c2)
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps [2]
    :helps-value [:communalism]
    :significance :lowest}

   {:type :action
    :name :discuss-shared-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c1 :values ?v] [?c2 :values ?v]
                    [(not= ?c1 ?c2)]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps [0 2]
    :helps-value [:communalism 4] ; helps the shared value in addition to communalism
    :significance :low}

   {:type :action
    :name :praise-own-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c1 :values ?v]
                    [?c2 :type :char] ; need this so we don't sometimes get past actions as ?c2
                    [(not= ?c1 ?c2)] ; TODO this isn't actually working
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [4]
    :significance :lowest}

   {:type :action
    :name :praise-others-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c2 :values ?v]
                    [?c1 :type :char] ; need this so we don't sometimes get past actions as ?c1
                    [(not= ?c1 ?c2)] ; TODO this isn't actually working
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [:communalism 4]
    :significance :lowest}

   {:type :action
    :name :disparage-others-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c2 :values ?v]
                    [?c1 :type :char] ; need this so we don't sometimes get past actions as ?c1
                    [(not= ?c1 ?c2)] ; TODO this isn't actually working
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism 4]
    :significance :lowest}

   {:type :action
    :name :make-request
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :type :char] [?c2 :type :char]
                    [(not= ?c1 ?c2)]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :significance :low}

   {:type :action
    :name :confide-in
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?liking :charge :like] [?liking :src ?c1] [?liking :dst ?c2]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [:communalism]
    :significance :low}

   {:type :action
    :name :notice-presence
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :type :char] [?c2 :type :char]
                    [(not= ?c1 ?c2)]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :significance :lowest}

   {:type :action
    :name :assert-authority
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :role :professor] [?c2 :type :char]
                    [(not= ?c1 ?c2)]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [:order]
    :significance :lowest}

   {:type :action
    :name :undermine-authority
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :type :char] [?c2 :role :professor]
                    [(not= ?c1 ?c2)]
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:order]
    :significance :low}

   ;; movement?

   {:type :action
    :name :flee
    :query '[:find ?c1 ?n1 :in $ %
             :where [?c1 :name ?n1]]
    :significance :lowest}

   ;; actions that require special traits

   {:type :action
    :name :use-secret-expertise
    :query '[:find ?c1 ?n1 :in $ %
             :where [?c1 :trait "secret expert"] [?c1 :name ?n1]]
    :significance :high}

   {:type :action
    :name :confess-to
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (crush ?c1 ?c2) [?c1 :name ?n1] [?c2 :name ?n2]]
    :significance :high}

   {:type :action
    :name :tweet-about-this
    :query '[:find ?c ?n :in $ %
             :where (or [?c :role :journalist]
                        [?c :trait "celebrity"]
                        [?c :trait "important online"])
                    [?c :name ?n]]
    :significance :lowest}

   {:type :action
    :name :do-science
    :query '[:find ?c ?n :in $ %
             :where (scientist ?c) [?c :name ?n]]
    :helps-value [:progress :science]
    :significance :lowest}

   {:type :action
    :name :assist-with-research
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (scientist ?c1) (scientist ?c2)
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps [2]
    :helps-value [:communalism :progress :science]
    :significance :lowest}

   {:type :action
    :name :interfere-with-research
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (nonscientist ?c1) (scientist ?c2)
                    [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism :progress :science]
    :significance :low}

])

(defn action->str [action]
  (str (name (:name action)) " " (pr-str (:vars action))))

(defn print-actions! [actions]
  (println "==== ACTIONS: ====")
  (doseq [action actions]
    (println (action->str action)))
  (println "=================="))

(defn available-actions [db]
  (mapcat (fn [action]
            (let [varsets (d/q (:query action) db rules)]
              (map #(assoc (dissoc action :query) :vars %) varsets)))
          all-actions))

(defn draw-actions
  ([db] (draw-actions db 5))
  ([db limit]
    (let [actions (vec (take limit (shuffle (available-actions db))))]
      (print-actions! actions)
      actions)))

(defn draw-actions-for-char
  ([db char] (draw-actions-for-char db char 5))
  ([db char limit]
    (let [char (->id db char)
          actions
          (->> (available-actions db)
               (filter #(= (first (:vars %)) char))
               (shuffle)
               (take limit)
               (vec))]
      (print-actions! actions)
      actions)))

(defn perform-action! [conn action]
  (let [perp-id       (first (:vars action))
        harmed-ids    (map #(nth (:vars action) %) (:harms action))
        helped-ids    (map #(nth (:vars action) %) (:helps action))
        harmed-values (map #(if (integer? %) (nth (:vars action) %) %) (:harms-value action))
        helped-values (map #(if (integer? %) (nth (:vars action) %) %) (:helps-value action))
        action        (assoc action
                        :perp perp-id
                        :harms harmed-ids
                        :helps helped-ids
                        :harms-value harmed-values
                        :helps-value helped-values)]
    (d/transact! conn [action])))

(defn gen-world! []
  (let [cast (gen-cast)]
    (d/reset-conn! conn (d/empty-db schema))
    (d/transact! conn cast)
    (doseq [char cast :when (map? char)]
      (prn char))))

(defn -main []
  (gen-world!))
