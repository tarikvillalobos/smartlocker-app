import XCTest

final class SmartLockerUITests: XCTestCase {
    func testStartupKeyboardAndRotationPreserveContact() {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        defer { XCUIDevice.shared.orientation = .portrait }
        let app = XCUIApplication()
        app.launchArguments = ["-smartlocker.environment", "DEMO"]
        app.launch()
        waitForOrientation(portrait: true, in: app)
        let phone = app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@", "Celular")).firstMatch
        XCTAssertTrue(phone.waitForExistence(timeout: 15))
        reveal(phone, in: app)
        phone.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        waitUntilUncovered(phone, in: app)
        app.typeText("11987654321")
        XCUIDevice.shared.orientation = .landscapeLeft
        waitForOrientation(portrait: false, in: app)
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        waitUntilUncovered(phone, in: app)
        XCTAssertEqual(phone.value as? String, "11987654321")
        XCTAssertGreaterThan(app.frame.width, app.frame.height)
        let landscape = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        landscape.name = "Landscape with keyboard"
        landscape.lifetime = .keepAlways
        add(landscape)
        XCUIDevice.shared.orientation = .portrait
        waitForOrientation(portrait: true, in: app)
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        waitUntilUncovered(phone, in: app)
        XCTAssertEqual(phone.value as? String, "11987654321")
        let portrait = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        portrait.name = "Portrait with keyboard"
        portrait.lifetime = .keepAlways
        add(portrait)
        app.terminate()
    }

    private func waitForOrientation(portrait: Bool, in app: XCUIApplication) {
        let rotated = NSPredicate { _, _ in
            let frame = app.frame
            return portrait ? frame.height > frame.width : frame.width > frame.height
        }
        let expectation = XCTNSPredicateExpectation(predicate: rotated, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [expectation], timeout: 8), .completed,
                       "Application geometry did not finish rotating: \(app.frame)")
    }

    private func waitUntilUncovered(_ field: XCUIElement, in app: XCUIApplication) {
        let visible = NSPredicate { _, _ in
            field.frame.maxY <= app.keyboards.firstMatch.frame.minY && field.frame.height > 0
        }
        let expectation = XCTNSPredicateExpectation(predicate: visible, object: nil)
        let result = XCTWaiter.wait(for: [expectation], timeout: 8)
        if result != .completed {
            let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            screenshot.name = "Keyboard overlap failure"
            screenshot.lifetime = .keepAlways
            add(screenshot)
        }
        XCTAssertEqual(result, .completed,
                       "Field \(field.frame), keyboard \(app.keyboards.firstMatch.frame), app \(app.frame)")
    }

    private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<6 {
            if element.isHittable { return }
            app.swipeUp()
        }
        XCTAssertTrue(element.isHittable)
    }
}
