The sum algorithm in the following were found to be exploitable:
* sqlite
* beam
* mysql
* python's sum
* others?

Any backends based on the following are more difficult to exploit:
* spark
* postgres
* numpy (so long as the dataset len is greater than one block)
* generally any pairwise or kahan sum algorithm


# Chorus
* adjusted files:
    * chorus/src/main/scala/examples/MechanismExamples.scala
    * chorus/src/test/scala/resources/schema.yml
    * chorus/pom.xml (to add JDBC drivers)
* usually ran via:

        mvn package -Dmaven.test.skip && mvn -e exec:java -Dexec.mainClass="examples.MechanismExamples" -Dexec.cleanupDaemonThreads=false
    * -Dmaven.test.skip to skip tests
    * -Dexec.cleanupDaemonThreads=false to avoid confusing errors when cleaning up Postgres and MySql threadpools
* security:
    * vulnerable to the 64-bit sized sum attack with just 17 records
    * it would also be vulnerable to the unsized sum attack if the library switched, 
        because you can connect it to a MySql backend
    

# Google Privacy on Beam
* new folder:
    * differential-privacy/privacy-on-beam/codelab_sum/
* check the readme in the new folder
* security:
    * beam too slow to handle the 64-bit dataset, and would crash before exploit finished
        * need to either run attack on a beefy computer, or fix the memory usage
    * cannot run attack on cluster, because order is important to preserve
    * cannot switch to 32-bit floats, because the accumulator is 64-bit (nice!) 


# PINQ
* adjusted file:
    * PINQ/TestHarness/TestHarness.cs
* installed Visual Studio Code to get it to run
* security:
    * vulnerable to the 64-bit unsized sum attack
    * too memory inefficient to also run the transform
    * accumulator is fixed to 64-bit floats


# PipelineDP
* notebook:
    * attack_pipelinedp.ipynb
* security:
    * uses a sized sum sensitivity to compute the numerator of the mean
        * only valid to use this sensitivity when dataset size is known
        * some potential to further exploit via methods in this paper
    * also vulnerable to the unsized sensitivity attack when using the beam backend
        * beam is too slow on limited hardware to execute the attack successfully
        * should be viable with greater resources
    * has a map and flatmap in the privacy layer with unbounded stability
        * these would also permit transformations on the data to make it more susceptible


# Google Differential Privacy / PyDP
* notebook:
    * attack_openmined_mean.ipynb
* security:
    * demonstrated vulnerability to sized sum attack
    * some doubts about correctness of dividing by two in sized sum


# IBM DiffPrivLib
* notebook:
    * attack_IBM_exact_count.ipynb
* security:
    * demonstrated vulnerability to sized sum attack


# SmartNoise SQL
* python file:
    * attack_smartnoise_sql.py
* security:
    * cannot currently be configured with sqlite or mysql, so the unsized sum attack is more difficult to run
    * disallows row transforms, so data cannot be manipulated to a form that is more susceptible


# OpenDP "Contrib"
* security:
    * `make_sized_bounded_sum` vulnerable to sized sensitivity attack
    * `make_bounded_sum` vulnerable to unsized sensitivity attack
