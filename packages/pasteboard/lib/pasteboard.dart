import 'dart:async';
import 'dart:typed_data';

import 'src/pasteboard_platform_web.dart'
    if (dart.library.io) 'src/pasteboard_platform_io.dart';

class Pasteboard {
  /// Returns the image data of the pasteboard.
  ///
  /// available on iOS, Desktop, Web, Android
  static Future<Uint8List?> get image => pasteboard.image;

  /// only available on Windows and the web.
  /// Get "HTML format" from system pasteboard.
  /// HTML format: https://docs.microsoft.com/en-us/previous-versions/windows/internet-explorer/ie-developer/platform-apis/aa767917(v=vs.85)
  static Future<String?> get html => pasteboard.html;

  /// available on iOS, Android, Web
  ///
  /// set image data to system pasteboard.
  static Future<void> writeImage(Uint8List? image) =>
      pasteboard.writeImage(image);

  /// available on Desktop, Android.
  ///
  /// Android: paths are content uris, must read by content resolver.
  ///
  /// Get files from system pasteboard.
  static Future<List<String>> files() => pasteboard.files();

  /// Only available on desktop platforms.
  ///
  /// Set files to system pasteboard.
  static Future<bool> writeFiles(List<String> files) =>
      pasteboard.writeFiles(files);

  /// Available on all platforms.
  /// Get text from system pasteboard.
  static Future<String?> get text => pasteboard.text;

  /// Available on all platforms.
  /// Set text to system pasteboard.
  static void writeText(String value) => pasteboard.writeText(value);
}
