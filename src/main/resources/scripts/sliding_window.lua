-- KEYS[1]: Rate limit key
-- ARGV[1]: Current epoch timestamp in milliseconds
-- ARGV[2]: Window duration in milliseconds
-- ARGV[3]: Maximum request limit

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local clearBefore = now - window

-- Remove outdated elements older than sliding window
redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)

-- Count surviving requests
local currentRequests = redis.call('ZCARD', key)

if currentRequests < limit then
    -- Add unique member (timestamp + random salt to handle sub-ms concurrency)
    redis.call('ZADD', key, now, now .. '-' .. math.random(100000, 999999))
    redis.call('PEXPIRE', key, window)
    return {1, limit - currentRequests - 1, 0}
else
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    local retryAfter = 1
    if oldest and #oldest >= 2 then
        retryAfter = math.ceil((tonumber(oldest[2]) + window - now) / 1000)
        if retryAfter < 1 then retryAfter = 1 end
    end
    return {0, 0, retryAfter}
end