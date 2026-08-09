package com.aistudio.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plugin_pack_members")
@IdClass(PluginPackMemberEntity.Pk.class)
@Getter
@Setter
public class PluginPackMemberEntity {

    @Id
    @Column(name = "pack_id", length = 64)
    private String packId;

    @Id
    @Column(name = "plugin_id", length = 80)
    private String pluginId;

    public static class Pk implements Serializable {
        private String packId;
        private String pluginId;

        public Pk() {
        }

        public Pk(String packId, String pluginId) {
            this.packId = packId;
            this.pluginId = pluginId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pk pk = (Pk) o;
            return Objects.equals(packId, pk.packId) && Objects.equals(pluginId, pk.pluginId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packId, pluginId);
        }
    }
}
