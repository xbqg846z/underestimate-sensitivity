package examples

import chorus.schema.Schema
import chorus.schema.Database
import chorus.sql.QueryParser
import chorus.mechanisms.LaplaceMechClipping
import chorus.mechanisms.AverageMechClipping
import chorus.mechanisms.EpsilonCompositionAccountant
import chorus.rewriting.RewriterConfig


object MechanismExamples extends App {

  def simple_sized_attack(database: Database) = {
    val config = new RewriterConfig(database)

    // Define simple test queries
    val query_u = "SELECT SUM(col1) FROM sized_64_4_trans_u"
    val query_v = "SELECT SUM(col1) FROM sized_64_4_trans_v"
    val root_u = QueryParser.parseToRelTree(query_u, database)
    val root_v = QueryParser.parseToRelTree(query_v, database)

    // Define the privacy accountant
    val accountant = new EpsilonCompositionAccountant()

    val mech_u = new LaplaceMechClipping(1.0, .5000000000000008, .5000000000000009, root_u, config)
    val mech_v = new LaplaceMechClipping(1.0, .5000000000000008, .5000000000000009, root_v, config)
    val limit = 10000
    // Run the mechanisms
    val counts_u = (1 to limit).map(_ => mech_u.execute(accountant).head).groupBy(identity).mapValues(_.size)
    val counts_v = (1 to limit).map(_ => mech_v.execute(accountant).head).groupBy(identity).mapValues(_.size)

    println("Sum(u) " + counts_u)
    println("Sum(v) " + counts_v)

    // Sum(u) Map(Row(List(8.500000000000012)) -> 9994, Row(List(8.500000000000014)) -> 3, Row(List(8.50000000000001)) -> 3)
    // Sum(v) Map(Row(List(8.500000000000012)) -> 1, Row(List(8.500000000000014)) -> 9998, Row(List(8.500000000000016)) -> 1)

    // cleaned up:
    // Sum(u) | 9994, 3 |
    // Sum(v) | 1, 9998 |
  }

  def where_sized_attack(database: Database) = {
    // this attack doesn't work because chorus disallows IN in WHERE clauses
    val config = new RewriterConfig(database)

    // individuals 2, 8, ... are known to be false, based on information the attacker collected
    // individual 45 is the target
    val poll = "(2, 8, 10, 12, 13, 20, 23, 24, 25, 27, 33, 35, 39, 40, 41, 43, 45)";
    // dataset of size 2**6, where uid 45 differs
    val query_u = "SELECT SUM(val) FROM sized_6_45_u WHERE uid IN " + poll;
    val query_v = "SELECT SUM(val) FROM sized_6_45_v WHERE uid IN " + poll;
    val root_u = QueryParser.parseToRelTree(query_u, database)
    val root_v = QueryParser.parseToRelTree(query_v, database)

    // Define the privacy accountant
    val accountant = new EpsilonCompositionAccountant()

    // Run the mechanisms
    val sum_u = new LaplaceMechClipping(1.0, .5000000000000008, .5000000000000009, root_u, config).execute(accountant)
    val sum_v = new LaplaceMechClipping(1.0, .5000000000000008, .5000000000000009, root_v, config).execute(accountant)

    println("Sum(u) " + sum_u)
    println("Sum(v) " + sum_v)

    println("Total privacy cost: " + accountant.getTotalCost())
  }

  System.setProperty("dp.elastic_sensitivity.check_bins_for_release", "false")
  // System.setProperty("dp.use_dummy_database", "true")

  // sqlite doesn't seem to work as a backend because calcite doesn't support it
  // System.setProperty("db.driver", "org.sqlite.JDBC")
  // System.setProperty("db.url", "jdbc:sqlite:/Users/michael/openDP/prelim/sensitivity_attacks/database.sqlite3")
  // System.setProperty("db.username", "")
  // System.setProperty("db.password", "")

  System.setProperty("db.driver", "com.mysql.cj.jdbc.Driver")
  System.setProperty("db.url", "jdbc:mysql://127.0.0.1:3306")
  System.setProperty("db.username", "root")
  System.setProperty("db.password", "")

  // postgres can function as a backend, but it doesn't have the easily exploitable sum algorithm
  // System.setProperty("db.driver", "org.postgresql.Driver")
  // System.setProperty("db.url", "jdbc:postgresql://127.0.0.1:5432/sensitivity_attack")
  // System.setProperty("db.username", "postgres") 
  // System.setProperty("db.password", "new_password")

  // Use the table schemas and metadata defined by the test classes
  System.setProperty("schema.config.path", "src/test/resources/schema.yaml")
  val database = Schema.getDatabase("sensitivity_attack")

  simple_sized_attack(database)
  // where_sized_attack(database)
}
