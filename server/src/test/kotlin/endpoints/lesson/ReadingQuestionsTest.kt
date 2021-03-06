package endpoints.lesson

import com.fasterxml.jackson.databind.JsonNode
import endpoints.EndpointTestCase
import endpoints.IntegrationEndpointTestCase
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert.assertThat
import org.http4k.unquoted
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class ReadingQuestionsTest : IntegrationEndpointTestCase() {

    @After
    override fun tearDown() {
        super.tearDown()
        File(Paths.get(environment.extractsPath, "test.txt").toUri()).delete()
    }

    @Test
    fun canGetRqWithInlineExtract() {
        testDatabaseAdaptor.runQuery("""
                CREATE (c:Course {name: "Course", image: "img.png"})-[:HAS_TOPIC_LESSON {index: 0}]->(l:TopicLesson {name: "RQ"})
                CREATE (l)-[:HAS_QUESTION {index: 0}]->(rq:Question:ReadingQuestion {extractInline: "inline-extract"})
                CREATE (rq)-[:HAS_SUBQUESTION {index: 0}]->(rsq:ReadingSubQuestion {given:"What does 'საქართველო' mean in English?", answer:"Georgia"})
                RETURN l,rq,rsq,c;
                """)

        val responseJson = lessonRequestJson("Course", "RQ")
        val questions = responseJson["questions"]
        assertThat(questions.size(), equalTo(1))

        val rq = questions[0]
        assertThat(rq["type"].asInt(), equalTo(2))
        assertThat(rq["index"].asInt(), equalTo(0))
        assertThat(rq["extract"].toString().unquoted(), equalTo("inline-extract"))
    }

    @Test
    fun canGetAnRsq() {
        testDatabaseAdaptor.runQuery("""
                CREATE (c:Course {name: "Course", image: "img.png"})-[:HAS_TOPIC_LESSON {index: 0}]->(l:TopicLesson {name: "RQ"})
                CREATE (l)-[:HAS_QUESTION {index: 0}]->(rq:Question:ReadingQuestion {extractInline: "inline-extract"})
                CREATE (rq)-[:HAS_SUBQUESTION {index: 0}]->(rsq:ReadingSubQuestion {given:"What does 'საქართველო' mean in English?", answer:"Georgia"})
                RETURN l,rq,rsq,c;
                """)

        val responseJson = lessonRequestJson("Course", "RQ")
        val questions = responseJson["questions"]
        assertThat(questions.size(), equalTo(1))

        val rq = questions[0]
        val subquestions = rq["questions"]
        assertThat(subquestions.size(), equalTo(1))

        val rsq = subquestions[0]
        assertThat(rsq["given"].toString().unquoted(), equalTo("What does 'საქართველო' mean in English?"))
        assertThat(rsq["answer"].toString().unquoted(), equalTo("Georgia"))
        assertFalse(rsq.has("answers"))
    }

    @Test
    fun canGetRqWithFileExtract() {
        File(Paths.get(environment.extractsPath, "test.txt").toUri()).writeText("file-extract")

        testDatabaseAdaptor.runQuery("""
                CREATE (c:Course {name: "Course", image: "img.png"})-[:HAS_TOPIC_LESSON {index: 0}]->(l:TopicLesson {name: "RQ"})
                CREATE (l)-[:HAS_QUESTION {index: 0}]->(rq:Question:ReadingQuestion {extractFile: "test.txt"})
                RETURN l,rq,c;
                """)

        val responseJson = lessonRequestJson("Course", "RQ")
        val questions = responseJson["questions"]
        assertThat(questions.size(), equalTo(1))

        val rq = questions[0]
        assertThat(rq["type"].asInt(), equalTo(2))
        assertThat(rq["index"].asInt(), equalTo(0))
        assertThat(rq["extract"].toString().unquoted(), equalTo("file-extract"))
    }

    @Test
    fun canGetAnRsqWithMultipleAnswers() {
        testDatabaseAdaptor.runQuery("""
                CREATE (c:Course {name: "Course", image: "img.png"})-[:HAS_TOPIC_LESSON {index: 0}]->(l:TopicLesson {name: "RQ"})
                CREATE (l)-[:HAS_QUESTION {index: 0}]->(rq:Question:ReadingQuestion {extractInline: "inline-extract"})
                CREATE (rq)-[:HAS_SUBQUESTION {index: 0}]->(rsq:ReadingSubQuestion {given:"What does 'ის მოხალისეა' mean in English?", answers:["She is a volunteer", "She's a volunteer", "He is a volunteer", "He's a volunteer"]})
                RETURN l,rq,rsq,c;
                """)

        val responseJson = lessonRequestJson("Course", "RQ")
        val questions = responseJson["questions"]
        assertThat(questions.size(), equalTo(1))

        val rq = questions[0]
        val subquestions = rq["questions"]

        val rsq = subquestions[0]
        assertThat(rsq["given"].toString().unquoted(), equalTo("What does 'ის მოხალისეა' mean in English?"))
        val answers = rsq["answers"].map { s -> s.toString().unquoted() }
        assertThat(answers, hasItem("She is a volunteer"))
        assertThat(answers, hasItem("She's a volunteer"))
        assertThat(answers, hasItem("He is a volunteer"))
        assertThat(answers, hasItem("He's a volunteer"))
        assertThat(answers.size, equalTo(4))
        assertFalse(rsq.has("answer"))
    }

    @Test
    fun canGetAnRqWithMultipleRsqs() {
        testDatabaseAdaptor.runQuery("""
                CREATE (c:Course {name: "Course", image: "img.png"})-[:HAS_TOPIC_LESSON {index: 0}]->(l:TopicLesson {name: "RQ"})
                CREATE (l)-[:HAS_QUESTION {index: 0}]->(rq:Question:ReadingQuestion {extractInline: "inline-extract"})
                CREATE (rq)-[:HAS_SUBQUESTION {index: 0}]->(rsq1:ReadingSubQuestion {given:"What does 'ის მოხალისეა' mean in English?", answers:["She is a volunteer", "She's a volunteer", "He is a volunteer", "He's a volunteer"]})
                CREATE (rq)-[:HAS_SUBQUESTION {index: 1}]->(rsq2:ReadingSubQuestion {given:"What does 'საავადმყოფო' mean in English?", answers: ["the hospital", "hospital"]})
                CREATE (rq)-[:HAS_SUBQUESTION {index: 2}]->(rsq3:ReadingSubQuestion {given:"What does 'გმადლობ' mean in English?", answer: "thanks!"})
                CREATE (rq)-[:HAS_SUBQUESTION {index: 3}]->(rsq4:ReadingSubQuestion {given:"What does 'გული' mean in English?", answer: "heart"})
                RETURN l,rq,rsq1,rsq2,rsq3,rsq4,c;
                """)

        val responseJson = lessonRequestJson("Course", "RQ")
        val questions = responseJson["questions"]

        val rq = questions[0]
        val subquestions = rq["questions"]
        assertThat(subquestions.size(), equalTo(4))

        assertThat(
            subquestionWithIndex(subquestions, 0)["given"].toString().unquoted(),
            equalTo("What does 'ის მოხალისეა' mean in English?")
        )
        assertThat(
            subquestionWithIndex(subquestions, 1)["given"].toString().unquoted(),
            equalTo("What does 'საავადმყოფო' mean in English?")
        )
        assertThat(
            subquestionWithIndex(subquestions, 2)["given"].toString().unquoted(),
            equalTo("What does 'გმადლობ' mean in English?")
        )
        assertThat(
            subquestionWithIndex(subquestions, 3)["given"].toString().unquoted(),
            equalTo("What does 'გული' mean in English?")
        )
    }

    private fun subquestionWithIndex(subquestions: JsonNode, index: Int): JsonNode {
        return subquestions.first { rsq -> rsq["index"].asInt() == index }
    }
}