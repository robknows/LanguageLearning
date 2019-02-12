// React
import React, {Component} from "react";
// Resources
import '../styles/CourseButton.css'

export default class CourseButton extends Component {
    constructor(props) {
        super(props)
        this.img = this.readImg(this.props.image)

        this.href = this.makeHref(window.location.pathname)
    }

    readImg(image) {
        if (image.type === "svg") {
            let svgB64 = btoa(image.src)
            return <img className="Course-icon" alt={this.props.courseName} src={"data:image/svg+xml;base64," + svgB64} />
        } else if (image.type === "png") {
            return <img className="Course-icon" alt={this.props.courseName} src={"data:image/png;base64," + image.src} />
        } else if (image.type === "jpg") {
            return <img className="Course-icon" alt={this.props.courseName} src={"data:image/jpeg;base64," + image.src} />
        }
    }

    makeHref(pathname) {
        if (pathname[pathname.length - 1] === "/") {
            return this.props.courseName
        } else {
            return "courses/" + this.props.courseName
        }
    }

    render() {
        return (
            <div>
                <h3 className="Course-title">{this.props.courseName}</h3>
                <a
                    id={"course-link-" + this.props.courseName}
                    href={this.href}
                    onClick={() => this.props.analytics.recordEvent("click@course-button-" + this.props.courseName.toLowerCase())}
                >
                    {this.img}
                </a>
                <br/>
                <br/>
            </div>
        )
    }
}
