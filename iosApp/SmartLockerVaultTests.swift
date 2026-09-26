import XCTest
@testable import SmartLockerHost

final class SmartLockerVaultTests: XCTestCase {
    func testSessionRoundTripUpdateAndRepeatedLogout() {
        let key = "test.smartlocker.\(UUID().uuidString)"
        defer { XCTAssertTrue(Keychain.write(key, nil)) }
        XCTAssertNil(Keychain.read(key))
        XCTAssertTrue(Keychain.write(key, "synthetic-token-ação\nsecond line"))
        XCTAssertTrue(Keychain.read(key) == "synthetic-token-ação\nsecond line")
        XCTAssertTrue(Keychain.write(key, "replacement-token"))
        XCTAssertTrue(Keychain.read(key) == "replacement-token")
        XCTAssertTrue(Keychain.write(key, nil))
        XCTAssertTrue(Keychain.write(key, nil))
        XCTAssertNil(Keychain.read(key))
    }

    func testDeletingOneScopePreservesAnother() {
        let first = "test.smartlocker.\(UUID().uuidString)"
        let second = "\(first).another-brand"
