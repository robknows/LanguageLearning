package main

import (
	"io/ioutil"
	"log"
	"strings"

	driver "github.com/johnnadratowski/golang-neo4j-bolt-driver"
	"github.com/johnnadratowski/golang-neo4j-bolt-driver/structures/graph"
)

// ====== Lesson =========

func QueryLesson(lessonName string) Lesson {
	lessonIndex := QueryLessonIndex(lessonName)

	cypher := `MATCH (tl:TopicLesson {name: {name}})-[r:HAS_QUESTION]->(q) RETURN q,r.index`
	params := map[string]interface{}{"name": lessonName}
	rows, conn, stmt := performQuery(cypher, params)
	defer conn.Close()
	defer stmt.Close()

	var questions []JsonEncodable
	row, _, err := rows.NextNeo()
	for row != nil && err == nil {
		parsedQuestion := parseQuestion(lessonName, row)
		questions = append(questions, parsedQuestion)
		row, _, err = rows.NextNeo()
	}

	return Lesson{Name: lessonName, Questions: questions, Index: lessonIndex}
}

func QueryLessonIndex(lessonName string) int64 {
	cypher := `MATCH (tl:TopicLesson {name: {name}})<-[r:HAS_TOPIC_LESSON]-(c:Course) RETURN r.index`
	params := map[string]interface{}{"name": lessonName}
	rows, conn, stmt := performQuery(cypher, params)
	defer conn.Close()
	defer stmt.Close()
	row, _, err := rows.NextNeo()
	if err != nil {
		log.Printf("%v", err)
		log.Printf("There was a problem getting the index of lesson %#v", lessonName)
		panic("neo4jdatabase:QueryLessonIndex")
	}
	return row[0].(int64)
}

func parseQuestion(lessonName string, row []interface{}) JsonEncodable {
	node := firstNode(row)
	if hasLabel(node, "TranslationQuestion") {
		return parseTQ(row)
	} else if hasLabel(node, "MultipleChoiceQuestion") {
		return parseMCQ(row)
	} else if hasLabel(node, "ReadingQuestion") {
		return parseRQ(lessonName, row)
	} else {
		log.Printf("Couldn't find any appropriate question type label on node %#v within row %#v", node, row)
		panic("neo4jdatabase:parseQuestion")
	}
}

func hasLabel(node graph.Node, keyLabel string) bool {
	for _, label := range node.Labels {
		if label == keyLabel {
			return true
		}
	}
	return false
}

func toStrings(list []interface{}) []string {
	var result []string
	for _, elem := range list {
		result = append(result, elem.(string))
	}
	return result
}

func parseTQ(row []interface{}) JsonEncodable {
	node := firstNode(row)
	p := node.Properties
	index := row[1].(int64)
	if answer, isSATQ := p["answer"]; isSATQ {
		return NewSATQ(index, p["given"].(string), answer.(string))
	} else if answers, isMATQ := p["answers"]; isMATQ {
		return NewMATQ(index, p["given"].(string), toStrings(answers.([]interface{})))
	} else {
		log.Printf("TQ node had neither answer nor answers property")
		panic("neo4jdatabase:parseTQ")
	}
}

func parseMCQ(row []interface{}) JsonEncodable {
	node := firstNode(row)
	index := row[1].(int64)
	p := node.Properties
	a := "!"
	b := "!"
	c := "!"
	d := "!"
	if choiceA, hasA := p["a"]; hasA {
		a = choiceA.(string)
	}
	if choiceB, hasB := p["b"]; hasB {
		b = choiceB.(string)
	}
	if choiceC, hasC := p["c"]; hasC {
		c = choiceC.(string)
	}
	if choiceD, hasD := p["d"]; hasD {
		d = choiceD.(string)
	}
	return NewMCQ(index, p["question"].(string), a, b, c, d, p["answer"].(string))
}

func parseRQ(lessonName string, row []interface{}) JsonEncodable {
	node := firstNode(row)
	p := node.Properties
	index := row[1].(int64)
	extract := parseRQExtract(p)

	cypher := "MATCH (tl:TopicLesson {name: {lessonName}})-[:HAS_QUESTION {index: {index}}]->(rq:ReadingQuestion)-[r:HAS_SUBQUESTION]->(rsq:ReadingSubQuestion) RETURN rsq,r.index"
	params := map[string]interface{}{"lessonName": lessonName, "index": index}
	rows, conn, stmt := performQuery(cypher, params)
	defer conn.Close()
	defer stmt.Close()

	return NewRQ(index, extract, parseJsonEncodableRows(rows, parseRSQ))
}

func parseRQExtract(nodeProperties map[string]interface{}) string {
	if inlineExtract, hasInlineExtract := nodeProperties["extractInline"]; hasInlineExtract {
		return inlineExtract.(string)
	} else if extractFileRelPath, hasExtractFile := nodeProperties["extractFile"]; hasExtractFile {
		path := strings.Join([]string{ExtractsPath, extractFileRelPath.(string)}, "")
		bytes, err := ioutil.ReadFile(path)
		if err != nil {
			log.Printf("Couldn't read RQ extract from path %#v", path)
			panic("neo4jdatabase:parseRQExtract")
		}
		return string(bytes)
	} else {
		log.Printf("RQ node has neither extractInline nor extractFile property")
		panic("neo4jdatabase:parseRQExtract")
	}
}

func parseRSQ(row []interface{}) JsonEncodable {
	node := firstNode(row)
	index := row[1].(int64)
	p := node.Properties
	if answer, isSARSQ := p["answer"]; isSARSQ {
		return NewSARSQ(index, p["given"].(string), answer.(string))
	} else if answers, isMARSQ := p["answers"]; isMARSQ {
		return NewMARSQ(index, p["given"].(string), toStrings(answers.([]interface{})))
	} else {
		log.Printf("RSQ node had neither answer nor answers property")
		panic("neo4jdatabase:parseRSQ")
	}
}

// ====== Common =========

type JsonEncodableParse func([]interface{}) JsonEncodable

func parseJsonEncodableRows(rows driver.Rows, parse JsonEncodableParse) []JsonEncodable {
	var encodables []JsonEncodable
	row, _, err := rows.NextNeo()
	for row != nil && err == nil {
		parsedEncodable := parse(row)
		encodables = append(encodables, parsedEncodable)
		row, _, err = rows.NextNeo()
	}
	return encodables
}

func openConnection() driver.Conn {
	conn, err := driver.NewDriver().OpenNeo(Neo4jURL)
	if err != nil {
		log.Printf("Error opening neo4j connection")
		conn.Close()
		panic("neo4jdatabase:openConnection")
	}
	return conn
}

func prepareStatement(conn driver.Conn, cypher string) driver.Stmt {
	stmt, err := conn.PrepareNeo(cypher)
	if err != nil {
		log.Printf("Error preparing cypher query statement")
		panic("neo4jdatabase:prepareStatement")
	}
	return stmt
}

func executeStatement(stmt driver.Stmt, params map[string]interface{}) driver.Rows {
	rows, err := stmt.QueryNeo(params)
	if err != nil {
		log.Printf("Error performing query")
		panic("neo4jdatabase:executeStatement")
	}
	return rows
}

func performQuery(cypher string, params map[string]interface{}) (driver.Rows, driver.Conn, driver.Stmt) {
	conn := openConnection()
	stmt := prepareStatement(conn, cypher)
	rows := executeStatement(stmt, params)
	return rows, conn, stmt
}

func firstNode(row []interface{}) graph.Node {
	node := row[0].(graph.Node)
	return node
}
