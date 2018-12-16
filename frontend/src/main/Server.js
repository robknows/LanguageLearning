// Main
import config from './config'

const defaultFetcher = {
    getJSON: (url) => {
        return fetch(url).then(response => response.json())
    },

    postJSON: (url, body) => {
        return fetch(url, {
            method: 'POST',
            body: JSON.stringify(body)
        }).then(response => response.json())
    }
}

// From: https://stackoverflow.com/questions/16648076/sort-array-on-key-value
function keySort(key) {
    return function(a, b){
        if (a[key] > b[key]) return 1
        if (a[key] < b[key]) return -1
        return 0
    }
}

function Server(backendOrigin, fetcher) {
    return {
        fetchLessonNames: (courseName) => {
            return fetcher.getJSON(backendOrigin + "/coursemetadata?course=" + courseName)
                .then(metadata => {
                    metadata.lessonMetadata.sort(keySort("index"))
                    return {topicLessonNames: metadata.lessonMetadata.map(l => l.name)}
                })
        },

        fetchCourseMetadata: (courseName) => {
            return fetcher.getJSON(backendOrigin + "/coursemetadata?course=" + courseName)
        },

        fetchLesson: (lessonName) => {
            return fetcher.postJSON(backendOrigin + "/lesson", {lessonName: lessonName}).then(lesson => {
                return {
                    name: lesson.name,
                    questions: lesson.questions
                }
            })
        },

        fetchCourses: () => {
            return fetcher.getJSON(backendOrigin + "/courses")
        }
    }
}

export const LocalServer = (fetcher) => Server(config.localBackendOrigin, fetcher)
export const defaultServer = LocalServer(defaultFetcher)
