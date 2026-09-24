package arbiter.workspace;

/** Why a registered source could not be read. */
public enum SourceFailure {
    /** The stored path is missing, blank, or not a workspace-relative path. */
    INVALID_PATH,
    /** No file exists at the recorded location. */
    MISSING,
    /** The recorded location is not a regular file. */
    NOT_A_FILE,
    /** The recorded location is not a plain-text file. */
    NOT_TEXT,
    /** The file is larger than the size this version reads. */
    TOO_LARGE,
    /** The location resolves outside the workspace's media folder. */
    OUTSIDE_MEDIA,
    /** The file exists but cannot be read. */
    UNREADABLE,
    /** The file's bytes no longer match the hash recorded at registration. */
    HASH_MISMATCH,
    /** The file's bytes are not valid UTF-8 text. */
    INVALID_TEXT
}
