-- This function is called by wrk on every request.
request = function()
    wrk.method = "POST"

    wrk.body = '{"message": "This is a benchmark payload!"}'

    wrk.headers["Content-Type"] = "application/json"

    return wrk.format()
end