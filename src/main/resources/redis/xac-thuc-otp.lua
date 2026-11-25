local state = redis.call('GET', KEYS[1])
if not state then
    return 0
end
local digest, attempts = string.match(state, '^([0-9a-f]+):(%d+)$')
if not digest then
    return 0
end
if digest == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
attempts = tonumber(attempts) + 1
if attempts >= 5 then
    redis.call('DEL', KEYS[1])
else
    redis.call('SET', KEYS[1], digest .. ':' .. attempts, 'KEEPTTL')
end
return 0
