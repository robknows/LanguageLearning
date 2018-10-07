import os
import time

import requests
from assertpy import assert_that

QHOME = os.environ["QHOME"]


def start_backend_q():
    os.system(QHOME + "/l32/q backend.q &")


def kill_backend_q():
    os.system("pkill -f \"" + QHOME + "/l32/q backend.q\"")


def lesson_names():
    res = requests.get("http://localhost:8000/lessonnames")
    assert_that(res.status_code).is_equal_to(200)
    assert_that(res.json()).is_equal_to(["hello", "what are you called?"])


def preflight_cors_request():
    headers = { "access-control-request-method": "GET" , "access-control-request-headers": "Content-Type" }
    options = requests.options("http://localhost:8000/lessonnames", headers=headers)
    assert_that(options.headers["Access-Control-Allow-Origin"]).is_equal_to("http://localhost:3000")
    assert_that(options.headers["Access-Control-Allow-Methods"]).is_equal_to("GET")
    assert_that(options.headers["Access-Control-Allow-Headers"]).is_equal_to("Content-Type")
    assert_that(options.status_code).is_equal_to(200)


def run_test(test_name, test):
    try:
        print("- " + test_name)
        test()
    except AssertionError as e:
        print("fail\n\t" + str(e))
    except Exception as e:
        print("error\n\t" + str(e))



def tests():
    run_test("lesson_names", lesson_names)
    run_test("preflight_cors_request", preflight_cors_request)


def main():
    print("=== starting backend ===")
    start_backend_q()
    time.sleep(1)

    print("=== starting tests ===")
    tests()

    print("=== killing backend ===")
    kill_backend_q()

    print("=== finished tests ===")


main()
exit(0)
