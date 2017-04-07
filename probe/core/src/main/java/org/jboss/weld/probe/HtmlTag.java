package org.jboss.weld.probe;

import static org.jboss.weld.probe.Strings.CHEVRONS_LEFT;
import static org.jboss.weld.probe.Strings.CHEVRONS_RIGHT;
import static org.jboss.weld.probe.Strings.EQUALS;
import static org.jboss.weld.probe.Strings.ID;
import static org.jboss.weld.probe.Strings.QUTATION_MARK;
import static org.jboss.weld.probe.Strings.SLASH;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple HTML tag builder.
 *
 * @author Martin Kouba
 */
class HtmlTag {

    static final String HTML = "html";
    static final String BODY = "body";
    static final String TITLE = "title";
    static final String H = "h";
    static final String P = "p";
    static final String PRE = "pre";
    static final String DIV = "div";
    static final String TABLE = "table";
    static final String TR = "tr";
    static final String TD = "td";
    static final String TH = "th";
    static final String STYLE = "style";
    static final String HEAD = "head";
    static final String A = "a";
    static final String OL = "ol";
    static final String LI = "li";
    static final String STRONG = "strong";
    static final String BR = "<br>";
    static final String CLASS = "class";

    static HtmlTag of(String name) {
        return new HtmlTag(name);
    }

    static HtmlTag html() {
        return of(HTML);
    }

    static HtmlTag head() {
        return of(HEAD);
    }

    static HtmlTag style() {
        return of(STYLE);
    }

    static HtmlTag body() {
        return of(BODY);
    }

    static HtmlTag title(String value) {
        return of(TITLE).add(value);
    }

    static HtmlTag h1(String value) {
        return h(1, value);
    }

    static HtmlTag h2(String value) {
        return h(2, value);
    }

    static HtmlTag h(int level, String value) {
        return of(H + level).add(value);
    }

    static HtmlTag p(String value) {
        return of(P).add(value);
    }

    static HtmlTag pre(String value) {
        return of(PRE).add(value);
    }

    static HtmlTag div(String id) {
        return div().attr(ID, id);
    }

    static HtmlTag div() {
        return of(DIV);
    }

    static HtmlTag table() {
        return of(TABLE);
    }

    static HtmlTag stripedTable() {
        return of(TABLE).attr(CLASS, "table-striped");
    }

    static HtmlTag tr() {
        return of(TR);
    }

    static HtmlTag th(String value) {
        return of(TH).add(value);
    }

    static HtmlTag td() {
        return of(TD);
    }

    static HtmlTag td(String value) {
        return td().add(value);
    }

    static HtmlTag a(String href) {
        return of(A).attr("href", href);
    }

    static HtmlTag aname(String name) {
        return of(A).attr(Strings.NAME, name);
    }

    static HtmlTag ol() {
        return of(OL);
    }

    static HtmlTag li() {
        return of(LI);
    }

    static HtmlTag strong(String text) {
        return of(STRONG).add(text);
    }

    private final String name;

    private Map<String, String> attrs;

    private final List<Object> contents;

    private HtmlTag(String name) {
        this.name = name;
        this.contents = new LinkedList<>();
    }

    HtmlTag attr(String name, String value) {
        if (attrs == null) {
            attrs = new HashMap<>();
        }
        attrs.put(name, value);
        return this;
    }

    /**
     * Add the contents and return the tag.
     *
     * @param content
     * @return self
     */
    HtmlTag add(Object... contents) {
        for (Object content : contents) {
            this.contents.add(content);
        }
        return this;
    }

    /**
     * Add the given tag and return it.
     *
     * @param tag
     * @return the added tag
     */
    HtmlTag addAndGet(HtmlTag tag) {
        add(tag);
        return tag;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(CHEVRONS_LEFT);
        builder.append(name);
        if (attrs != null && !attrs.isEmpty()) {
            for (Entry<String, String> attr : attrs.entrySet()) {
                builder.append(" ");
                builder.append(attr.getKey());
                builder.append(EQUALS);
                builder.append(QUTATION_MARK);
                builder.append(attr.getValue());
                builder.append(QUTATION_MARK);
            }
        }
        builder.append(CHEVRONS_RIGHT);
        for (Object content : contents) {
            builder.append(content.toString());
        }
        builder.append(CHEVRONS_LEFT);
        builder.append(SLASH);
        builder.append(name);
        builder.append(CHEVRONS_RIGHT);
        return builder.toString();
    }

}