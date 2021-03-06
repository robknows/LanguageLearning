// React
import React from 'react'
// Testing
import {mount, shallow} from 'enzyme'
// Main
import Lesson from '../../main/Lesson'
import {nonShuffler} from "../../main/Shuffler"
// Enzyme react-adapter configuration & others
import {configureAdapter, sleep} from "../utils"

configureAdapter()

let mockServerLoadTimeMs = 1

let mockServer = lesson => {
    return {
        fetchLesson: () => {
            return new Promise(resolve => resolve(lesson))
        }
    }
}

async function shallowRenderLesson(course, lessonName, server) {
    let lesson = shallow(<Lesson courseName={course} encodedLessonName={encodeURIComponent(lessonName)} server={server} shuffler={nonShuffler} />)
    await sleep(mockServerLoadTimeMs)
    return lesson
}

it('Shows the lesson name from the lesson data', async () => {
    // Given: I am in a Japanese lesson called hello
    let testServer = mockServer({name: "Hello!", questions: [{type: -1}]})
    let testLesson = await shallowRenderLesson("japanese", "Hello!", testServer)

    // When: I look at the title
    let title = testLesson.find('h1').first()
    let text = title.children().map(child => child.text()).reduce((acc, cur) => acc + cur)

    // Then: I see that the lesson name is displayed with the course name
    expect(text).toBe("Japanese: Hello!")
})

it('Shows the loading screen while loading', async () => {
    let mockSlowServer = lesson => {
        return {
            fetchLesson: () => {
                return new Promise(async resolve => {
                    await sleep(500)
                    resolve(lesson)
                })
            }
        }
    }

    // Given: The server is slow
    let slowServer = mockSlowServer({name: "Boxing"})

    // When: I am in a lesson
    let testLesson = await shallowRenderLesson("Thai", "Boxing", slowServer)

    // Then: The page indicates that it is loading
    let title = testLesson.find('h1').first()
    let text = title.children().map(child => child.text()).reduce((acc, cur) => acc + cur)

    expect(text).toBe("Loading Thai: Boxing")
})

async function mountRenderLesson(course, lessonName, server) {
    let lesson = mount(<Lesson courseName={course} encodedLessonName={encodeURIComponent(lessonName)} server={server} shuffler={nonShuffler} />)
    await sleep(mockServerLoadTimeMs)
    lesson.update()
    return lesson
}

it('Fetches the lesson from the server based on the course name and lesson name', async () => {
    let testServer = {
        fetchLessonCalledWithCourseName: null,
        fetchLessonCalledWithLessonName: null,
        fetchLesson: (courseName, lessonName) => {
            testServer.fetchLessonCalledWithCourseName = courseName
            testServer.fetchLessonCalledWithLessonName = lessonName
            return new Promise(resolve => resolve({questions: []}))
        }
    }

    await mountRenderLesson("Georgian", "Hello", testServer)

    expect(testServer.fetchLessonCalledWithCourseName).toEqual("Georgian")
    expect(testServer.fetchLessonCalledWithLessonName).toEqual("Hello")
})
