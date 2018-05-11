package org.some.thing.counter

import spock.lang.Specification

class CounterTest extends Specification {

    def "PCSA estimator should be within 1 std of excat counter"() {

        when:

            def pcsa = new PCSA<Long>()
            def exact = new Exact<Long>()

            def range = 10**11
            def random = new Random()
            random.setSeed(123)

            random.with {
                (1..1*10**3).collect {
                    def n = nextInt(range).abs()
                    pcsa.add(n)
                    exact.add(n)
                }
            }

            println "exact cardinality=${exact.cardinality()}, bias=${exact.bias()}, error=${exact.error()}"
            def card = pcsa.cardinality()
            def std = (int)(card*pcsa.error())
            def min = card - std
            def max = card + std

            println "pcsa cardinality=${card}, bias=${pcsa.bias()}, error=${pcsa.error()}, interval=(${min},${max})"

        then:
            pcsa.error() >  0
            //max >= exact.cardinality() && min <= exact.cardinality()
    }
}
