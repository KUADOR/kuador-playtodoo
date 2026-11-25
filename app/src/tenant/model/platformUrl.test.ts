import { describe, expect, it } from 'vitest'
import { resolvePlatformUrl } from './platformUrl'

const location = (hostname: string, port = '', protocol = 'https:') => ({ protocol, hostname, port })

describe('resolvePlatformUrl', () => {
  it('uses the configured URL when there is one', () => {
    expect(resolvePlatformUrl('https://staging.example.com/', location('elsol.playtodoo.com'))).toBe(
      'https://staging.example.com/',
    )
  })

  it('falls back to the root domain of the tenant host', () => {
    expect(resolvePlatformUrl(undefined, location('elsol.playtodoo.com'))).toBe('https://playtodoo.com/')
  })

  it('keeps protocol and port of a local tenant host', () => {
    expect(resolvePlatformUrl('', location('elsol.localhost', '5173', 'http:'))).toBe('http://localhost:5173/')
  })

  it('keeps a host that has no subdomain', () => {
    expect(resolvePlatformUrl(undefined, location('localhost', '5173', 'http:'))).toBe('http://localhost:5173/')
  })
})
