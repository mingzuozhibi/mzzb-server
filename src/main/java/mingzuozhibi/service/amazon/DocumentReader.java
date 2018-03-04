package mingzuozhibi.service.amazon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class DocumentReader {

    public static Node getNode(Document document, String... names) {
        Node node = null;
        for (String name : names) {
            if (document == null) break;
            node = document.getElementsByTagName(name).item(0);

            if (node == null) break;
            document = node.getOwnerDocument();
        }
        return node;
    }

    public static String getText(Document document, String... names) {
        Node node = getNode(document, names);
        return node != null ? node.getTextContent() : null;
    }

}
