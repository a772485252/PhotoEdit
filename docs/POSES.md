# Offline pose picker

24 independently authored front-view mannequin vectors (18 standing, 6 seated).
All legs are separated and do not cross. Artwork uses Android VectorDrawable;
there are no bitmap originals, remote image calls or new dependencies.

The RuiC-PoseAtlas LICENSE scope notice explicitly excludes img/ and thumbs/
and prohibits redistribution of the image data. No images or metadata from
that project are bundled. Source reviewed 2026-11-05:
https://github.com/HRuiCcc/RuiC-PoseAtlas/blob/main/LICENSE

Select AI 图片 → 选姿势 → a card → 使用姿势. This fills an editable Chinese
pose paragraph and does not generate or upload anything. Existing text and
face reference stay in place. Selecting another pose replaces the managed
【姿势】…【/姿势】 paragraph instead of accumulating conflicting poses.
The diagrams are illustrative; text-only pose matching depends on the model.

Device checks: offline browse, standing/seated filters, selection/preview,
cancel, repeated selection preserving other prompt text and face reference,
and manually triggered generation for each provider. Small screens and larger
font settings should also be checked.
