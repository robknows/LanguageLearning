package neo4j

import org.neo4j.driver.internal.InternalNode
import org.neo4j.driver.internal.value.IntegerValue
import org.neo4j.driver.internal.value.ListValue
import org.neo4j.driver.internal.value.MapValue
import org.neo4j.driver.internal.value.StringValue
import org.neo4j.driver.v1.*
import org.neo4j.driver.v1.types.Node
import java.util.*

open class Neo4jDriver(user: String, password: String, boltPort: Int) {
    private val uri = "bolt://$user:$password@localhost:$boltPort"
    val driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))

    open fun queryValues(query: String): List<Value> {
        return driver.session().readTransaction { tx -> tx.run(query).list() }.map { record -> record.valueInColumn(0) }
    }

    open fun queryTwoValuesWithParams(query: String, params: Map<String, Any>): List<Pair<Value, Value>> {
        return driver.session().readTransaction { tx -> tx.run(query, params).list() }
            .map { record: Record -> Pair(record.valueInColumn(0), record.valueInColumn(1)) }
    }

    fun session(): Session {
        return driver.session()
    }

    fun queryValuesWithParams(query: String, params: Map<String, Any>): List<Value> {
        return driver.session().readTransaction { tx -> tx.run(query, params).list() }
            .map { record -> record.valueInColumn(0) }
    }
}

fun Record.valueInColumn(columnIndex: Int): Value {
    return this[columnIndex]
}

fun stringValue(s: String): Value {
    return StringValue(s)
}

fun intValue(i: Int): Value {
    return IntegerValue(i.toLong())
}

fun mapValue(vararg pairs: Pair<String, Value>): Value {
    val hashMap = HashMap<String, Value>()
    for (pair in pairs) {
        hashMap[pair.first] = pair.second
    }
    return MapValue(hashMap)
}

fun listValue2(v1: Value, v2: Value): Value {
    return ListValue(v1, v2)
}

fun neo4jCourseValue(courseName: String, imageFileRelativePath: String): Value {
    return mapValue("name" to stringValue(courseName), "image" to stringValue(imageFileRelativePath))
}

fun neo4jNode(labels: List<String>, properties: Map<String, Value>): Node {
    return InternalNode(0, labels, properties)
}
