package paper.example;

public class ComplexClass {
    private static final String MANIFEST_NAME = "MANIFEST";
    public Resource[][] gradManifests(Resource[] rcs) {
        Resource[][] manifests = new Resource[rcs.length][];

        for (int i = 0; i < rcs.length; i++) {
            Resource[][] rec = null;
            if (rcs[i] instanceof FileSet) {
                rec = grabRes(new FileSet[]{(FileSet) rcs[i]});
            } else {
                rec = grabNonFileSetRes(new Resource[]{rcs[i]});
            }
            for (int j = 0; j < rec[0].length; j++) {
                String name = rec[0][j].getName().replace('\\', '/');
                if (rcs[i] instanceof ArchiveFileSet) {
                    i++;
                    ArchiveFileSet afs = (ArchiveFileSet) rcs[i];
                    if (!"".equals(afs.getFullPath(getProj()))) {
                        name = afs.getFullPath(getProj());
                    } else if (!"".equals(afs.getPref(getProj()))) {
                        String pr = afs.getPref(getProj());
                        if (!pr.endsWith("/") && !pr.endsWith("\\")) {
                            pr += "/";
                        }
                        name = pr + name;
                    }
                }
                if (name.equalsIgnoreCase(MANIFEST_NAME)) {
                    manifests[i] = new Resource[]{rec[0][j]};
                    break;
                }
            }
            if (manifests[i] == null) {
                manifests[i] = new Resource[0];
            }
        }
        return manifests;
    }

    public Resource[][] grabRes(FileSet[] set) {
        return null;
    }

    public Resource[][] grabNonFileSetRes(Resource[] r) {
        return null;
    }

    public String getProj() {
        return null;
    }
}
