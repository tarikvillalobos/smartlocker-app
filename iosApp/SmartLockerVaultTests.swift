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
        defer {
            XCTAssertTrue(Keychain.write(first, nil))
            XCTAssertTrue(Keychain.write(second, nil))
        }
        XCTAssertTrue(Keychain.write(first, "first-synthetic-session"))
        XCTAssertTrue(Keychain.write(second, "second-synthetic-session"))
        XCTAssertTrue(Keychain.write(first, nil))
        XCTAssertNil(Keychain.read(first))
        XCTAssertTrue(Keychain.read(second) == "second-synthetic-session")
    }
}
