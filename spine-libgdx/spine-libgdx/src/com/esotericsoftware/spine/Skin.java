/******************************************************************************
 * Spine Runtimes Software License v2.5
 *
 * Copyright (c) 2013-2016, Esoteric Software
 * All rights reserved.
 *
 * You are granted a perpetual, non-exclusive, non-sublicensable, and
 * non-transferable license to use, install, execute, and perform the Spine
 * Runtimes software and derivative works solely for personal or internal
 * use. Without the written permission of Esoteric Software (see Section 2 of
 * the Spine Software License Agreement), you may not (a) modify, translate,
 * adapt, or develop new applications using the Spine Runtimes or otherwise
 * create derivative works or improvements of the Spine Runtimes or (b) remove,
 * delete, alter, or obscure any trademarks or any copyright, trademark, patent,
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 *
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES, BUSINESS INTERRUPTION, OR LOSS OF
 * USE, DATA, OR PROFITS) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.esotericsoftware.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;

/** Stores attachments by slot index and attachment name.
 * <p>
 * See SkeletonData {@link SkeletonData#defaultSkin}, Skeleton {@link Skeleton#skin}, and
 * <a href="http://esotericsoftware.com/spine-runtime-skins">Runtime skins</a> in the Spine Runtimes Guide. */
public class Skin {
	final String name;
	final OrderedMap<SkinEntry, Attachment> attachments = new OrderedMap();
	final Array<BoneData> bones = new Array();
	final Array<ConstraintData> constraints = new Array();
	private final SkinEntry lookup = new SkinEntry();

	public Skin (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		this.name = name;
		this.attachments.orderedKeys().ordered = false;
	}

	/** Adds an attachment to the skin for the specified slot index and name. */
	public void setAttachment (int slotIndex, String name, Attachment attachment) {
		if (attachment == null) throw new IllegalArgumentException("attachment cannot be null.");
		if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
		attachments.put(new SkinEntry(slotIndex, name, attachment), attachment);
	}

	/** Adds all attachments, bones, and constraints from the specified skin to this skin. */
	public void addSkin (Skin skin) {
		for (BoneData data : skin.bones)
			if (!bones.contains(data, true)) bones.add(data);

		for (ConstraintData data : skin.constraints)
			if (!constraints.contains(data, true)) constraints.add(data);

		for (SkinEntry entry : skin.attachments.keys())
			setAttachment(entry.getSlotIndex(), entry.getName(), entry.getAttachment());
	}

	/** Adds all attachments, bones, and constraints from the specified skin to this skin. Attachments are deep copied. */
	public void copySkin (Skin skin) {
		for (BoneData data : skin.bones)
			if (!bones.contains(data, true)) bones.add(data);

		for (ConstraintData data : skin.constraints)
			if (!constraints.contains(data, true)) constraints.add(data);

		for (SkinEntry entry : skin.attachments.keys()) {
			Attachment attachment = entry.getAttachment().copy();
			setAttachment(entry.getSlotIndex(), entry.getName(), attachment);
		}

		for (SkinEntry entry : attachments.keys()) {
			Attachment attachment = entry.getAttachment();
			if (attachment instanceof MeshAttachment) {
				MeshAttachment mesh = (MeshAttachment)attachment;
				if (mesh.getParentMesh() != null) {
					mesh.setParentMesh((MeshAttachment)getAttachment(entry.getSlotIndex(), mesh.getParentMesh().getName()));
					mesh.updateUVs();
				}
			}
		}
	}

	/** Returns the attachment for the specified slot index and name, or null. */
	public Attachment getAttachment (int slotIndex, String name) {
		if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
		lookup.set(slotIndex, name, null);
		return attachments.get(lookup);
	}

	/** Removes the attachment in the skin for the specified slot index and name, if any. */
	public void removeAttachment (int slotIndex, String name) {
		if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must be >= 0.");
		lookup.set(slotIndex, name, null);
		attachments.remove(lookup);
	}

	/** Returns all attachments in this skin. */
	public Array<SkinEntry> getAttachments () {
		return attachments.orderedKeys();
	}

	/** Returns all attachments for the given slot in this skin. */
	public void getAttachments (int slotIndex, Array<SkinEntry> attachments) {
		for (SkinEntry entry : this.attachments.keys())
			if (entry.getSlotIndex() == slotIndex) attachments.add(entry);
	}

	public void clear () {
		attachments.clear(1024);
		bones.clear();
		constraints.clear();
	}

	public Array<BoneData> getBones () {
		return bones;
	}

	public Array<ConstraintData> getConstraints () {
		return constraints;
	}

	public int size () {
		return attachments.size;
	}

	/** The skin's name, which is unique within the skeleton. */
	public String getName () {
		return name;
	}

	public String toString () {
		return name;
	}

	/** Attach each attachment in this skin if the corresponding attachment in the old skin is currently attached. */
	void attachAll (Skeleton skeleton, Skin oldSkin) {
		for (SkinEntry entry : oldSkin.attachments.keys()) {
			int slotIndex = entry.getSlotIndex();
			Slot slot = skeleton.slots.get(slotIndex);
			if (slot.attachment == entry.getAttachment()) {
				Attachment attachment = getAttachment(slotIndex, entry.getName());
				if (attachment != null) slot.setAttachment(attachment);
			}
		}
	}

	/** Stores an entry in the skin consisting of the slot index, name, and attachment **/
	public static class SkinEntry {
		private int slotIndex;
		private String name;
		private Attachment attachment;
		private int hashCode;

		SkinEntry () {
			set(0, "", null);
		}

		SkinEntry (int slotIndex, String name, Attachment attachment) {
			set(slotIndex, name, attachment);
		}

		void set (int slotIndex, String name, Attachment attachment) {
			if (name == null) throw new IllegalArgumentException("name cannot be null.");
			this.slotIndex = slotIndex;
			this.name = name;
			this.attachment = attachment;
			this.hashCode = name.hashCode() + slotIndex * 37;
		}

		public int getSlotIndex () {
			return slotIndex;
		}

		public String getName () {
			return name;
		}

		public Attachment getAttachment () {
			return attachment;
		}

		public int hashCode () {
			return hashCode;
		}

		public boolean equals (Object object) {
			if (object == null) return false;
			SkinEntry other = (SkinEntry)object;
			if (slotIndex != other.slotIndex) return false;
			if (!name.equals(other.name)) return false;
			return true;
		}

		public String toString () {
			return slotIndex + ":" + name;
		}
	}
}
