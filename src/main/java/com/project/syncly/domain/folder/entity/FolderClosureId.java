package com.project.syncly.domain.folder.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FolderClosureId implements Serializable {
    private Long ancestorId;
    private Long descendantId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FolderClosureId)) return false;
        FolderClosureId that = (FolderClosureId) o;
        return Objects.equals(ancestorId, that.ancestorId) &&
                Objects.equals(descendantId, that.descendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ancestorId, descendantId);
    }
}
