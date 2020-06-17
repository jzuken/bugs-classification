package org.ml_methods_group.common;


import java.io.Serializable;

public class Solution implements Serializable {
    private final String code;
    private final String id;
    private final String solutionId;
    private final Verdict verdict;

    public Solution() {
        this(null, "0", "0", null);
    }

    public Solution(String code, String id, String solutionId, Verdict verdict) {
        this.code = code;
        this.id = id;
        this.solutionId = solutionId;
        this.verdict = verdict;
    }

    public String getCode() {
        return code;
    }

    public String getId() {
        return id;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getSolutionId() {
        return solutionId;
    }

    public enum Verdict {
        OK, FAIL, UNKNOWN;

        public static Verdict valueOf(int id) {
            if (id == OK.ordinal()) {
                return OK;
            } else if (id == FAIL.ordinal()) {
                return FAIL;
            } else if (id == UNKNOWN.ordinal()) {
                return UNKNOWN;
            }
            throw new IllegalArgumentException("Unexpected enum id");
        }
    }

    @Override
    public String toString() {
        return "Solution (id: " + id +
                ", solution id: " + solutionId +
                ", verdict: " + verdict + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Solution solution = (Solution) o;

        if (!id.equals(solution.id)) return false;
        if (!solutionId.equals(solution.solutionId)) return false;
        if (!code.equals(solution.code)) return false;
        return verdict == solution.verdict;
    }

    @Override
    public int hashCode() {
        int result = code.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + solutionId.hashCode();
        result = 31 * result + verdict.hashCode();
        return result;
    }
}
