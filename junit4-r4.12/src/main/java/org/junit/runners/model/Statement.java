package org.junit.runners.model;


/**
 * Represents one or more actions to be taken at runtime in the course
 * of running a JUnit test suite.
 *
 * @since 4.5
 */
public abstract class Statement {
    /**
     * Run the action, throwing a {@code Throwable} if anything goes wrong.
     * evaluate: 评价;评估;估计
     */
    public abstract void evaluate() throws Throwable;
}