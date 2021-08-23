# Tutorial: Multi-Stakeholder Recommender Systems (MSRS)
### Introduction
-------------------
Recommender systems are able to produce a list of recommended items tailored to user preferences, while the end user is the only stakeholder in the system. However, there could be multiple stakeholders in several applications or domains, e.g., e-commerce, advertising, educations, dating, job seeking, and so forth. It is useful to produce the recommendations by balancing the needs of different stakeholders. This tutorial covers the introductions to multi-stakeholder recommender systems (MSRS), introduces multiple case studies, discusses the corresponding methods and challenges to develop MSRS. Particularly, a demo based on the MOEA framework will be given in the talk.

### Demo
-------------------
The demo shows case of utility-based multi-stakeholder recommender systems (UBMSRS) by using an educational data as case study.

* **Setting**: To simplify the problem, the demo performs a hold-out evaluation by using 70% as training and 30% as testing. Recommendation performance was evaluated based on F1 and NDCG over the top-N recommendations, while you can change the value of N in the configuration file "setting.conf". Items with rating > 3 are considered as "relevant" items in the top-N recommendations.
* **MOEA**: In terms of the multi-objective optimizers (MOO), we use the library [MOEA](https://github.com/MOEAFramework/MOEAFramework). We use the default learning parameters in MOEA. To further tune up the models, change the source codes by referring to the [Beginner's guide to the MOEA Framework](http://moeaframework.org/documentation.html) 
* **Baselines**: **UBRec** refers to the utility-based multi-criteria recommendation model which considers student preferences only. **Rankp** refers to the simple ranking based on the utility of items from the perspective of instructors or professors only. These baseline approaches will produce different objectives as shown at the end in the setting.conf
* **How to run**: Change the setting in setting.conf, and run "java -jar UBMSRS.jar -c setting.conf" by using JRE 8 or above

### Setting.conf
-------------------
* **data.path**: tell the demo where you data sets are. Make sure all necessary data are in the same folder.
* **expectation.learn**: turn on to learn student expectations by using MOO. Otherwise, load student expectations
* **runbaseline**: demo will run baseline approaches first, if it is turned on
* **expectation.filename**: assign the file of student expectations you want to load
* **topN**: assign the value of N for top-N recommendations
* **maxeval**: assign the maximal number of evaluations in the MOO
* **maxf1, maxndcg, ...**: these are the best performance from the baseline approaches. You do not need to change these values unless you found better baseline results

### Data Sets
-------------------
Data files and explanations
* **ratings_student.csv** and **ratings_instructor.csv** are the raw rating data given by students and the instructor respectively
* **ratings_student_train.csv** and **ratings_student_test.csv** are the splitted data by using 70% as training and 30% as testing
* **ratings_student_candidates.csv** contains the pair of users and items, while the items are the candidate items to be recommended according to the train/test split. We have used the independent biased matrix factorization to predict the multi-criteria ratings in <App, Data, Ease> in this data.
* **expectations_student_learned_by_UBRec_NDCG_0.214.csv** is the file of learned student expectations by using the baseline UBRec. The optimal NDCG is 0.214. You can also save a new one if you can learn better expectations.

We collected this educational data for multi-stakeholder recommendations, and the information of student identifications have been removed. If you reuse this data for the purpose of research, please cite the following papers.

* Yong Zheng, J.R. Toribio. The role of transparency in multi-stakeholder educational recommendations. User Model User-Adap Inter 31, 513–540 (2021).
* Yong Zheng, Nastaran Ghane, and Milad Sabouri. "Personalized Educational Learning with Multi-Stakeholder Optimizations." Adjunct Proceedings of the ACM conference on User Modelling, Adaptation and Personalization (UMAP). ACM. 2019.
* Yong Zheng. "Multi-Stakeholder Personalized Learning with Preference Corrections." Proceedings of the 18th IEEE International Conference on Advanced Learning Technologies (ICALT). IEEE. 2019.

### Tutorials
-------------------
We have tutorials at ACM RecSys 2019 and ACM CIKM 2019.

* Yong Zheng. "Multi-Stakeholder Recommendations: Case Studies, Methods and Challenges", Proceedings of the 13th ACM Conference on Recommender Systems, Copenhagen, Denmark, September 19th, 2019 **Note: there is a user study in this paper**
* Muthusamy Chelliah, Yong Zheng, Sudeshna Sarkar, Vishal Kakkar. "Recommendation for Multi-Stakeholders and through Neural Review Mining", Proceedings of the 28th ACM International Conference on Information and Knowledge Management (CIKM), Beijing, China, Nov, 2019
