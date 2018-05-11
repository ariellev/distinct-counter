package org.some.thing.commons

import spock.lang.Specification

class UtilsTest extends Specification {

    def "jGet vanilla input"() {

        when:
        def json = "{\"key\":\"dcba5742f8a6083a3a9\",\"ts\": \"some-illegal-timestamp\", \"ts\":1468244384}"

        then:
        Utils.jGet(json, "key", "[a-z0-9]{19}") == "dcba5742f8a6083a3a9"
        Utils.jGet(json, "ts", "[0-9]+") == "1468244384"

    }

    def "jGet real input"() {

        when:
        def key = "uid"
        def expected = ['dcba5742f8a6083a3a9', '9bfcae2f7c3a4ec2add', 'dcc2bbb5349b61cfdae', '1b018e18ecd3ca96555']
        def resource = "/stream.jsonl.head"
        def reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(resource)));
        def lines = reader.lines();

        then:
        def actual = lines.collect { Utils.jGet(it, "uid", "[a-z0-9]{19}") } take 4
        println actual
        actual == expected

    }
}
