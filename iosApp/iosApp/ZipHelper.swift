import Foundation
import ZIPFoundation

@objc class ZipHelper: NSObject {
    @objc static func unzip(fileAtPath: String, destination: String) -> Bool {
        let fileURL = URL(fileURLWithPath: fileAtPath)
        let destURL = URL(fileURLWithPath: destination)
        do {
            try FileManager.default.unzipItem(at: fileURL, to: destURL)
            return true
        } catch {
            print("Unzip error: \(error)")
            return false
        }
    }
}