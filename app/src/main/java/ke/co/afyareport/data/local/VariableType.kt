package ke.co.afyareport.data.local

/**
 * Supported field types for a register variable.
 *
 * CODED variables carry a list of CodeOption entries (code + label)
 * that are used both for data entry and for monthly reporting.
 *
 * VISIT_TYPE is a special system-managed type with fixed options
 * NEW and RETURN. It is used to distinguish first visits from
 * subsequent visits of the same patient within a reporting session.
 */
enum class VariableType {
    TEXT,
    NUMBER,
    DATE,
    BOOLEAN,
    CODED,
    VISIT_TYPE
}
