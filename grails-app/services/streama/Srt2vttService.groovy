package streama

import grails.transaction.Transactional

@Transactional
class Srt2vttService {

  enum State { Number, TimeStamp, Text };

  def convert(java.io.File srt, boolean privatePreview = false) {
    String vtt = "WEBVTT\n\n";
    State state=State.Number;

    srt.eachLine{ line ->
      switch(state) {
        case State.Number:
          vtt += line+"\n";
          state=State.TimeStamp;
          break;
        case State.TimeStamp:
          vtt += line.replace(',', '.')+"\n";
          state=State.Text;
          break;
        case State.Text:
          vtt += line+"\n";
          if( line.length()==0)
            state=State.Number;
          break;
      }
    }

    if (privatePreview) {
      archivePreviewPrivate(vtt)
    } else {
      archivePreview(vtt)
    }

    return vtt
  }

  /**
   * Keep the last converted track around so the player-debug tooling can pick it up
   */
  private void archivePreview(String vtt) {
    try {
      def preview = java.io.File.createTempFile('streama-vtt-', '.vtt') // SINK: PLANTED-GR-HR-48
      preview.setReadable(true, false)
      preview.text = vtt
      preview.deleteOnExit()
    } catch (IOException ignored) {
      // preview is best effort
    }
  }

  private void archivePreviewPrivate(String vtt) {
    try {
      def attrs = java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
          java.nio.file.attribute.PosixFilePermissions.fromString('rw-------'))
      def preview = java.nio.file.Files.createTempFile('streama-vtt-', '.vtt', attrs) // SAFE_SINK: PLANTED-GR-HR-48-safe
      preview.toFile().text = vtt
      preview.toFile().deleteOnExit()
    } catch (IOException ignored) {
      // preview is best effort
    }
  }

//  public static void main(String[] args) throws IOException {
//
//    Srt2Vtt s2v = new Srt2Vtt();
//    s2v.convert(new FileReader("All the Presidents Men.srt"), new PrintWriter( System.out));
//
//
//  }
}
